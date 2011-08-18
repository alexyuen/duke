
package no.priv.garshol.duke;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collection;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class JDBCLinkDatabase implements LinkDatabase {
  private String driverklass;
  private String jdbcuri;
  private Properties props;
  private Statement stmt;
  private static final SimpleDateFormat dtformat =
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  
  public JDBCLinkDatabase(String driverklass,
                          String jdbcuri,
                          Properties props) {
    this.driverklass = driverklass;
    this.jdbcuri = jdbcuri;
    this.props = props;
    this.stmt = JDBCUtils.open(driverklass, jdbcuri, props);

    try {
      verifySchema();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  
  public List<Link> getAllLinks() {
    return getChangesSince(0, 0, 0);
  }
  
  public List<Link> getChangesSince(long since) {
    return getChangesSince(since, 0, 0);
  }

  public List<Link> getChangesSince(long since, long before) {
    return getChangesSince(since, before, 0);
  }

  public List<Link> getChangesSince(long since, long before, int pagesize) {
    String where = "";
    if (since != 0 || before != 0)
      where = "where ";
    if (since != 0)
      where += "timestamp > TIMESTAMP '" + dtformat.format(since) + "'";
    if (before != 0) {
      if (since != 0)
        where += "and ";
      where += "timestamp <= TIMESTAMP '" + dtformat.format(before) + "'";
    }

    String limit = "";
    if (pagesize != 0)
      limit = " limit " + pagesize;
    
    return queryForLinks("select * from links " + where +
                         " order by timestamp desc" + limit);
  }
  
  public Collection<Link> getAllLinksFor(String id) {
    return queryForLinks("select * from links where " +
                         "id1 = '" + escape(id) + "' or " +
                         "id2 = '" + escape(id) + "'");
  }

  public void assertLink(Link link) {
    // (1) query to see if the link is already there
    // FIXME: use prepared statement
    try {
      Link existing = null;
      ResultSet rs = stmt.executeQuery("select * from links where " +
                                  "id1 = '" + escape(link.getID1()) + "' and " +
                                  "id2 = '" + escape(link.getID2()) + "'");
      if (rs.next()) {
        existing = makeLink(rs);
        rs.close();
        
        if (!link.overrides(existing))
          return; // the existing link rules, so we shut up and go away
      }
      rs.close();
      
      // (2) write link to database
      String query;
      if (existing != null)
        query = "update links set status = " + link.getStatus().getId() +
          "  , kind = " + link.getKind().getId() + 
          "  , timestamp = now() where id1 = '" + escape(link.getID1()) + "' " +
          "and id2 = '" + escape(link.getID2()) + "' ";
      else
        query = "insert into links values ('" + escape(link.getID1()) + "', " +
          "  '" + escape(link.getID2()) + "', " + link.getKind().getId() +
          "  , " + link.getStatus().getId() + ", now()) ";
      stmt.executeUpdate(query);
      
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Empties the link database. Used only for testing at the moment.
   */
  public void clear() {
    try {
      stmt.executeUpdate("delete from links");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void commit() {
    try {
      stmt.getConnection().commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void close() {
    JDBCUtils.close(stmt);
  }

  private void verifySchema() throws SQLException {
    ResultSet rs = stmt.executeQuery("select * from information_schema.tables where " +
                                     "table_name = 'LINKS'");
    boolean present = rs.next();
    rs.close();

    if (present)
      return;

    stmt.executeUpdate("create table LINKS ( " +
                       "  id1 varchar not null, " +
                       "  id2 varchar not null, " +
                       "  kind int not null, " +
                       "  status int not null, " +
                       "  timestamp timestamp not null) ");

    stmt.executeUpdate("create primary key on LINKS (id1, id2) ");
  }

  private String escape(String strval) {
    return strval.replace("'", "''");
  }

  private List<Link> queryForLinks(String query) {
    List<Link> links = new ArrayList();
    
    try {
      ResultSet rs = stmt.executeQuery(query);
      while (rs.next())
        links.add(makeLink(rs));
      rs.close(); // FIXME: finally
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return links;
  }

  private Link makeLink(ResultSet rs) throws SQLException {
    return new Link(rs.getString("id1"),
                    rs.getString("id2"),
                    LinkStatus.getbyid(rs.getInt("status")),
                    LinkKind.getbyid(rs.getInt("kind")),
                    rs.getTimestamp("timestamp").getTime());
  }
  
}