import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Properties;

import junit.framework.TestCase;

import org.jetel.connection.DBConnection;
import org.jetel.connection.SQLDataParser;
import org.jetel.data.DataRecord;
import org.jetel.data.RecordKey;
import org.jetel.exception.ComponentNotReadyException;
import org.jetel.exception.JetelException;
import org.jetel.lookup.DBLookupTable;
import org.jetel.main.runGraph;
import org.jetel.metadata.DataRecordMetadata;



public class DBLookupTest extends TestCase {
	
	DBLookupTable lookupTable;
	DataRecord customer, employee = null;
	SQLDataParser parser;
	DBConnection aDBConnection;
	@Override
	protected void setUp() throws ComponentNotReadyException, FileNotFoundException, SQLException{

		runGraph.initEngine("../cloveretl.engine/plugins", null);
		aDBConnection = new DBConnection("conn", "../cloveretl.engine/examples/koule_postgre.cfg");
		aDBConnection.init();

		Properties p= new Properties();
		p.put("sqlQuery", "select * from customer");
		DataRecordMetadata customerMetadata = aDBConnection.createMetadata(p);
		customer = new DataRecord(customerMetadata);
		customer.init();
		parser = new SQLDataParser("select * from customer");
		parser.init(customerMetadata);
		parser.setDataSource(aDBConnection);
		
		lookupTable = new DBLookupTable("MyLookup",aDBConnection,null,"select * from employee where last_name=?",0);
        lookupTable.init();
		RecordKey recordKey = new RecordKey(new String[]{"lname"}, customerMetadata);
		recordKey.init();
		lookupTable.setLookupKey(recordKey);

	}

	@Override
	protected void tearDown() throws Exception {
		parser.close();
		lookupTable.free();
	}
	
	public void test1() throws JetelException, ComponentNotReadyException{
		long start = System.currentTimeMillis();
		while ((parser.getNext(customer)) != null){
//			System.out.println("Looking pair for :\n" + customer);
			employee = lookupTable.get(customer);
			if (employee != null) {
				do {
//					System.out.println("Found record:\n" + employee);
				}while ((employee = lookupTable.getNext()) != null);
			}
		}
		System.out.println("Without cashing:");
		System.out.println("Total number searched: " + lookupTable.getTotalNumber());
		System.out.println("From cache found: " + lookupTable.getCacheNumber());
		System.out.println("Timing: " + (System.currentTimeMillis() - start));
		lookupTable.free();
		
		parser.setDataSource(aDBConnection);
		lookupTable.init();
		start = System.currentTimeMillis();
		while ((parser.getNext(customer)) != null){
//			System.out.println("Looking pair for :\n" + customer);
			employee = lookupTable.get(customer);
			if (employee != null) {
				do {
//					System.out.println("Found record:\n" + employee);
				}while ((employee = lookupTable.getNext()) != null);
			}
		}
		System.out.println("Without cashing:");
		System.out.println("Total number searched: " + lookupTable.getTotalNumber());
		System.out.println("From cache found: " + lookupTable.getCacheNumber());
		System.out.println("Timing: " + (System.currentTimeMillis() - start));
		lookupTable.free();
		
		parser.setDataSource(aDBConnection);
		lookupTable.setNumCached(1000, false);
		lookupTable.init();
		start = System.currentTimeMillis();
		while ((parser.getNext(customer)) != null){
//			System.out.println("Looking pair for :\n" + customer);
			employee = lookupTable.get(customer);
			if (employee != null) {
				do {
//					System.out.println("Found record:\n" + employee);
				}while ((employee = lookupTable.getNext()) != null);
			}
		}
		System.out.println("Cashing=1000, storeNulls=false:");
		System.out.println("Total number searched: " + lookupTable.getTotalNumber());
		System.out.println("From cache found: " + lookupTable.getCacheNumber());
		System.out.println("Timing: " + (System.currentTimeMillis() - start));
		lookupTable.free();
		
		parser.setDataSource(aDBConnection);
		lookupTable.setNumCached(1000, true);
		lookupTable.init();
		start = System.currentTimeMillis();
		while ((parser.getNext(customer)) != null){
//			System.out.println("Looking pair for :\n" + customer);
			employee = lookupTable.get(customer);
			if (employee != null) {
				do {
//					System.out.println("Found record:\n" + employee);
				}while ((employee = lookupTable.getNext()) != null);
			}
		}
		System.out.println("Cashing=1000, storeNulls=true:");
		System.out.println("Total number searched: " + lookupTable.getTotalNumber());
		System.out.println("From cache found: " + lookupTable.getCacheNumber());
		System.out.println("Timing: " + (System.currentTimeMillis() - start));
		lookupTable.free();

		parser.setDataSource(aDBConnection);
		lookupTable.setNumCached(3000, false);
		lookupTable.init();
		start = System.currentTimeMillis();
		while ((parser.getNext(customer)) != null){
//			System.out.println("Looking pair for :\n" + customer);
			employee = lookupTable.get(customer);
			if (employee != null) {
				do {
//					System.out.println("Found record:\n" + employee);
				}while ((employee = lookupTable.getNext()) != null);
			}
		}
		System.out.println("Cashing=3000, storeNulls=false:");
		System.out.println("Total number searched: " + lookupTable.getTotalNumber());
		System.out.println("From cache found: " + lookupTable.getCacheNumber());
		System.out.println("Timing: " + (System.currentTimeMillis() - start));
		lookupTable.free();
		
		parser.setDataSource(aDBConnection);
		lookupTable.setNumCached(3000, true);
		lookupTable.init();
		start = System.currentTimeMillis();
		while ((parser.getNext(customer)) != null){
//			System.out.println("Looking pair for :\n" + customer);
			employee = lookupTable.get(customer);
			if (employee != null) {
				do {
//					System.out.println("Found record:\n" + employee);
				}while ((employee = lookupTable.getNext()) != null);
			}
		}
		System.out.println("Cashing=3000, storeNulls=true:");
		System.out.println("Total number searched: " + lookupTable.getTotalNumber());
		System.out.println("From cache found: " + lookupTable.getCacheNumber());
		System.out.println("Timing: " + (System.currentTimeMillis() - start));
}
	
	public void test_getObject() throws ComponentNotReadyException{
		lookupTable.setLookupKey(new Object());
		employee = lookupTable.get(new String[]{"Nowmer"});
		assertNotNull(employee);
		employee = lookupTable.get(new String[]{"Spencer"});
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNull(employee);

		lookupTable.setNumCached(1000, true);
		lookupTable.init();
		String[] key = new String[1];
		key[0] = "Nowmer";
		employee = lookupTable.get(key);
		assertNotNull(employee);
		key[0] = "Spencer";
		employee = lookupTable.get(key);
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNull(employee);
		employee = lookupTable.get(key);
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNull(employee);
	}
	
	public void test_getStringt() throws ComponentNotReadyException{
		lookupTable.setLookupKey(new String[1]);
		employee = lookupTable.get("Nowmer");
		assertNotNull(employee);
		employee = lookupTable.get("Spencer");
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNull(employee);

		lookupTable.setNumCached(1000, true);
		lookupTable.init();
		employee = lookupTable.get("Nowmer");
		assertNotNull(employee);
		employee = lookupTable.get("Spencer");
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNull(employee);
		employee = lookupTable.get("Spencer");
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNotNull(employee);
		employee = lookupTable.getNext();
		assertNull(employee);
	}

}
