import dao.*;
import logic.Passwords;
import java.sql.*;
import java.nio.file.*;

public class IntegrationTests {
 static int passed;
 static void ok(boolean value,String label){if(!value)throw new AssertionError(label);System.out.println("PASS: "+label);passed++;}
 static String scalar(String sql)throws Exception{try(Connection c=Db.connect();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getString(1);}}
 static void sql(String sql)throws Exception{try(Connection c=Db.connect();Statement s=c.createStatement()){s.executeUpdate(sql);}}
 public static void main(String[] args)throws Exception{
  String url=System.getenv("FACBACK_DB_URL");
  if(url==null||!url.contains("/facback_integration_"))throw new IllegalStateException("Use isolated integration database only");
  System.out.println("MySQL "+scalar("SELECT VERSION()"));
  Database.registerStudent("RA_TEST_1","test-pass");
  ok(Passwords.isHashed(scalar("SELECT password FROM Users WHERE username='RA_TEST_1'")),"registration stores hash");
  ok(Database.login("RA_TEST_1","test-pass")!=null && Database.login("RA_TEST_1","wrong")==null,"correct and incorrect login");
  sql("INSERT INTO Users(username,password,role) VALUES('RA_LEGACY','legacy','STUDENT')");
  ok(Database.login("RA_LEGACY","legacy")!=null&&Passwords.isHashed(scalar("SELECT password FROM Users WHERE username='RA_LEGACY'")),"legacy login upgrades hash");
  Database.submitReview("RA_TEST_1",1,new int[]{4,4,4,4},"integration test");
  ok(scalar("SELECT COUNT(*) FROM Reviews").equals("1")&&scalar("SELECT COUNT(*) FROM Submissions").equals("1"),"review and cooldown persisted");
  boolean blocked=false;try{Database.submitReview("RA_TEST_1",1,new int[]{4,4,4,4},"duplicate");}catch(Database.TooSoonException e){blocked=true;}
  ok(blocked&&scalar("SELECT COUNT(*) FROM Reviews").equals("1"),"14 day cooldown rejects duplicate");
  boolean failed=false;try{Database.submitReview("RA_LEGACY",1,new int[]{0,4,4,4},"invalid rating");}catch(SQLException e){failed=true;}
  ok(failed&&scalar("SELECT COUNT(*) FROM Reviews").equals("1")&&scalar("SELECT COUNT(*) FROM Submissions").equals("1"),"failed review rolls back cooldown and review");
  ReportDao dao=new ReportDao();dao.insert("RA_TEST_1",1,"test.png","fixture path");
  var reports=dao.reportsForTeacher(2);
  ok(reports.size()==1&&dao.reportsForTeacher(1).isEmpty(),"report routed to owning teacher");int id=reports.get(0).id();
  blocked=false;try{dao.logOutcome(1,id,"Confirmed","wrong owner");}catch(ReportException e){blocked=true;}
  ok(blocked&&dao.outcomeHistory(id).isEmpty(),"wrong teacher refused without history write");
  dao.logOutcome(2,id,"Needs more evidence","first");
  ok(dao.reportsForTeacher(2).get(0).outcome().equals("Needs more evidence")&&dao.outcomeHistory(id).size()==1,"owner outcome and history persisted");
  dao.logOutcome(2,id,"Not confirmed","second");
  ok(dao.outcomeHistory(id).size()==2&&dao.reportsForTeacher(2).get(0).outcome().equals("Not confirmed"),"two outcomes retain history");
  blocked=false;try{dao.insert("RA_TEST_1",1,"",null);}catch(ReportException e){blocked=true;}
  ok(blocked,"empty media path rejected");
  Path bad=Files.writeString(Path.of("bad.txt"),"fixture");blocked=false;try{MediaStore.store(bad);}catch(ReportException e){blocked=true;}
  ok(blocked,"unsupported media extension rejected");Files.delete(bad);
  Path png=Path.of("fixture.png");javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",png.toFile());
  String stored=MediaStore.store(png);ok(Files.exists(Path.of(stored)),"media copied");MediaStore.discard(stored);ok(!Files.exists(Path.of(stored)),"discard removes media copy");Files.delete(png);
  System.out.println("TOTAL: "+passed+" passed, 0 failed");
 }
}
