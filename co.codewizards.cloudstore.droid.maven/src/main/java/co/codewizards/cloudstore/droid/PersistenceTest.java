//package co.codewizards.cloudstore.droid;
//
//
//import java.io.File;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.Collection;
//
//import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
//import co.codewizards.cloudstore.local.persistence.CopyModification;
//import co.codewizards.cloudstore.local.persistence.CopyModificationDAO;
//import co.codewizards.cloudstore.local.persistence.Modification;
//import co.codewizards.cloudstore.local.persistence.ModificationDAO;
//import co.codewizards.cloudstore.local.persistence.RemoteRepository;
//import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDAO;
//
//public class PersistenceTest extends AbstractTest{
//
//	private static final int modificationCount = 10;
//
//	public void run(){
//		try {
//			//clearDatabase();
//			getModifications();
//		} catch (TestException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//	}
//
//
////	@Test
//	public void getModifications() throws Exception {
//		File remoteRoot = newTestRepositoryLocalRoot("/Users/wilk/Test");
//		remoteRoot.mkdirs();
//		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
//	
//		if(localRepoManager == null){
//			throw new TestException("LocalRepoManager initialization failed.");
//		}
//		
//		LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
//		try {
//			RemoteRepository remoteRepository = new RemoteRepository();
//			remoteRepository.setLocalPathPrefix("");
//			remoteRepository.setPublicKey(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 });
//			remoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).makePersistent(remoteRepository);
//
//			CopyModificationDAO copyModificationDAO = transaction.getDAO(CopyModificationDAO.class);
//			for (int i = 0; i < modificationCount; ++i) {
//				CopyModification copyModification = new CopyModification();
//				copyModification.setRemoteRepository(remoteRepository);
//				copyModification.setFromPath("/from/" + i);
//				copyModification.setToPath("/to/" + i);
//				copyModification.setLength(100000);
//				copyModification.setSha1("TEST" + i);
//				copyModificationDAO.makePersistent(copyModification);
//			}
//			transaction.commit();
//		} finally {
//			transaction.rollbackIfActive();
//		}
//
//		localRepoManager.close();
//		localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);
//
//		transaction = localRepoManager.beginReadTransaction();
//		try {
//			RemoteRepository remoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getObjects().iterator().next();
//			ModificationDAO modificationDAO = transaction.getDAO(ModificationDAO.class);
//			Collection<Modification> modifications = modificationDAO.getModifications(remoteRepository);
//			
//			if(modifications.size() != modificationCount){
//				throw new TestException("Incorrect number of modifications.");
//			}
//			
//			System.out.println("*** Accessing fromPath and toPath ***");
//			for (Modification modification : modifications) {
//				System.out.println(modification);
//				if (modification instanceof CopyModification) {
//					CopyModification copyModification = (CopyModification)modification;
//					System.out.println(String.format("%s => %s",
//							copyModification.getFromPath(),
//							copyModification.getToPath()));
//				}
//			}
//			
//			transaction.commit();
//		} finally {
//			transaction.rollbackIfActive();
//		}
//
//		localRepoManager.close();
//	}
//	
////	private void clearDatabase(){
////		Connection connection = null;
////		try {
////			Class.forName("SQLite2.DelegateJDBCDriver");
////			connection = DriverManager.getConnection("jdbc:sqlite:/Users/wilk/Test/test.db");
////			JDBCDatabaseMetaData metadata = new JDBCDatabaseMetaData((JDBCConnection)connection);
////			ResultSet rs = metadata.getTables(null, null, null, null);
////			String deleteSql = "DELETE * FROM ";
////			
////			while(rs.next()){
////				String tableName = rs.getString("TABLE_NAME");
////				System.out.println(tableName);
////				PreparedStatement ps = connection.prepareStatement(deleteSql +
////						tableName);
////				ps.execute();
////				ps.close();
////			}
////			rs.close();
////			
////		} catch (SQLException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		} catch (ClassNotFoundException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		} finally{
////			if(connection != null){
////				try {
////					connection.close();
////				} catch (SQLException e) {
////					// TODO Auto-generated catch block
////					e.printStackTrace();
////				}
////			}
////		}
////		
////	}
//	
//	
//}
