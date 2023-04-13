package uk.ac.sanger.storelight;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import uk.ac.sanger.storelight.model.GridDirection;
import uk.ac.sanger.storelight.repo.StoreDB;
import uk.ac.sanger.storelight.requests.LocationIdentifier;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that run right through graphql api to (some kind of) database
 * @author dr6
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import({GraphQLTester.class})
public class IntegrationTests {
    @Autowired
    private GraphQLTester tester;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private StoreDB db;

    @Test
    @Transactional
    public void testCreateLocation() throws Exception {
        LocationIdentifier freezerLi = makeFreezer();
        Integer freezerId = freezerLi.getId();

        String getLocationQuery = tester.readResource("graphql/getlocation.graphql");
        for (String li : new String[] { "{id:"+freezerId+"}", "{barcode:\""+freezerLi.getBarcode()+"\"}"}) {
            String query = getLocationQuery.replace("{id:1}", li);
            Object response = tester.post(query);
            Object locData = chainGet(response, "data", "location");
            assertEquals(chainGet(locData, "barcode"), freezerLi.getBarcode());
            assertEquals(chainGet(locData, "name"), "Freezer Alpha");
            assertEquals(chainGet(locData, "id"), freezerId);
            assertEquals(chainGet(locData, "description"), "A freezer.");
            assertNull(chainGet(locData, "size"));
            assertNull(chainGet(locData, "parent"));
            assertThat(chainGetList(locData, "children")).isEmpty();
            assertThat(chainGetList(locData, "stored")).isEmpty();
        }

        String addSubLocationQuery = tester.readResource("graphql/addsublocation.graphql");
        addSubLocationQuery = addSubLocationQuery.replace("1#PARENT_ID", freezerId.toString());
        Object response = tester.post(addSubLocationQuery);
        Object locData = chainGet(response, "data", "addLocation");
        String barcode = chainGet(locData, "barcode");
        assertThat(barcode).startsWith("STO-");
        Integer id = chainGet(locData, "id");
        assertNotNull(id);
        String query = getLocationQuery.replace("{id:1}", "{id:"+id+"}");
        response = tester.post(query);
        locData = chainGet(response, "data", "location");
        assertEquals(barcode, chainGet(locData, "barcode"));
        assertEquals(id, chainGet(locData, "id"));
        assertNull(chainGet(locData, "description"));
        assertEquals(Map.of("numRows", 2, "numColumns", 3), chainGet(locData, "size"));
        assertEquals(freezerId, chainGet(locData, "parent", "id"));
        assertThat(chainGetList(locData, "children")).isEmpty();
        assertThat(chainGetList(locData, "stored")).isEmpty();

        refresh(freezerLi);
        response = tester.post(getLocationQuery.replace("{id:1}", "{id:"+freezerId+"}"));
        List<?> children = chainGetList(response, "data", "location", "children");
        assertThat(children).hasSize(1);
        assertEquals(id, chainGet(children, 0, "id"));

        response = tester.post(getLocationQuery.replace("{id:1}", "{id:"+id+"}"));
        assertEquals(freezerLi.getBarcode()+" Freezer Alpha / A2", chainGet(response, "data", "location", "qualifiedNameWithFirstBarcode"));

        String hierQuery = tester.readResource("graphql/hierarchy.graphql");
        response = tester.post(hierQuery.replace("{id:1}", "{id:"+id+"}"));
        List<Map<String, String>> results = chainGet(response, "data", "locationHierarchy");
        assertMap(results.get(0),"barcode", freezerLi.getBarcode(),
                "name", "Freezer Alpha",
                "description", "A freezer.",
                "address", null
        );
        assertMap(results.get(1), "barcode", barcode, "name", null, "description", null, "address", "A2");
    }

    private void assertMap(Map<String, String> map, String... kvs) {
        final int len = kvs.length;
        for (int i = 0; i < len; i += 2) {
            assertEquals(kvs[i+1], map.get(kvs[i]), kvs[i]);
        }
        assertThat(map).hasSize(len/2);
    }

    @Test
    @Transactional
    public void testEditLocation() throws Exception {
        LocationIdentifier freezerLi = makeFreezer();
        String addSubLocationMutation = tester.readResource("graphql/addsublocation.graphql");
        addSubLocationMutation = addSubLocationMutation.replace("1#PARENT_ID", freezerLi.getId().toString());
        Object response = tester.post(addSubLocationMutation);
        Integer id = chainGet(response, "data", "addLocation", "id");
        String editMutation = tester.readResource("graphql/editlocation.graphql")
                .replace("{id:1}", "{id:"+id+"}");
        response = tester.post(editMutation);
        Map<String,?> info = chainGet(response, "data", "editLocation");
        assertEquals(id, info.get("id"));
        assertEquals("New name", info.get("name"));
        assertEquals("I like describing things.", info.get("description"));
        assertEquals(Map.of("numRows", 10, "numColumns", 11), info.get("size"));
        assertEquals("F12", info.get("address").toString());
        String getLocationQuery = tester.readResource("graphql/getlocation.graphql")
                .replace("{id:1}", "{id:"+id+"}");
        response = tester.post(getLocationQuery);
        info = chainGet(response, "data", "location");
        assertEquals(id, info.get("id"));
        assertEquals("I like describing things.", info.get("description"));
        assertEquals(Map.of("numRows", 10, "numColumns", 11), info.get("size"));
        assertEquals("F12", info.get("address").toString());
        assertEquals(freezerLi.getId(), chainGet(info, "parent", "id"));
        assertEquals(GridDirection.DownRight.name(), chainGet(info, "direction"));
    }

    @Test
    @Transactional
    public void testStoreUnstoreBarcode() throws Exception {
        LocationIdentifier li = makeFreezer();
        String storeMutation = "mutation { storeBarcode(barcode: \"ITEM-1\", location: {id: "+li.getId()+"}) { barcode }}";
        Object response = tester.post(storeMutation);
        assertEquals("ITEM-1", chainGet(response, "data", "storeBarcode", "barcode"));
        String storeWithAddressMutation = "mutation { storeBarcode(barcode: \"ITEM-2\", location: {id: "+li.getId()+"}, address: \"A2\") { barcode }}";
        response = tester.post(storeWithAddressMutation);
        assertEquals("ITEM-2", chainGet(response, "data", "storeBarcode", "barcode"));
        String storedQuery = tester.readResource("graphql/getstored.graphql");
        String itemQuery = storedQuery.replace("[]", "[\"ITEM-1\",\"ITEM-2\",\"ITEM-0\"]");
        response = tester.post(itemQuery);
        List<Map<String,?>> stored = chainGet(response, "data", "stored");
        Map<String, Object> item1 = new HashMap<>();
        item1.put("barcode", "ITEM-1");
        item1.put("address", null);
        Map<String, Integer> loc = Map.of("id", li.getId());
        item1.put("location", loc);
        Map<String, Object> item2 = new HashMap<>();
        item2.put("barcode", "ITEM-2");
        item2.put("address", "A2");
        item2.put("location", loc);
        assertThat(stored).containsOnly(item1, item2);

        refresh(li);
        String getLocationQuery = tester.readResource("graphql/getlocation.graphql")
                .replace("{id:1}", "{id:"+li.getId()+"}");
        response = tester.post(getLocationQuery);
        stored = chainGet(response, "data", "location", "stored");
        assertThat(stored).containsOnly(Map.of("barcode", "ITEM-1"), Map.of("barcode", "ITEM-2"));

        String unstoreMutation = "mutation { unstoreBarcode(barcode: \"ITEM-1\") { location { id }}}";
        response = tester.post(unstoreMutation);
        assertEquals(li.getId(), chainGet(response, "data", "unstoreBarcode", "location", "id"));

        entityManager.flush(); // deletes aren't executed until flush or commit
        refresh(li);

        response = tester.post(getLocationQuery);
        stored = chainGet(response, "data", "location", "stored");
        assertThat(stored).containsOnly(Map.of("barcode", "ITEM-2"));
    }

    @Test
    @Transactional
    public void testStoreUnstoreBarcodes() throws Exception {
        LocationIdentifier li = makeFreezer();
        String storeMutation = "mutation { storeBarcodes(barcodes: [\"ITEM-1\",\"ITEM-2\"], location: {id:"+li.getId()
                +"}) { numStored, stored { barcode } }}";
        Object response = tester.post(storeMutation
                .replace("{id:1}", "{id:"+li.getId()+"}")
                .replace("[]", "[\"ITEM-1\", \"ITEM-2\"]")
        );
        assertEquals("ITEM-1", chainGet(response, "data", "storeBarcodes", "stored", 0, "barcode"));
        assertEquals((Integer) 2, chainGet(response, "data", "storeBarcodes", "numStored"));

        refresh(li);
        String getLocationQuery = tester.readResource("graphql/getlocation.graphql")
                .replace("{id:1}", "{id:"+li.getId()+"}");
        response = tester.post(getLocationQuery);
        assertThat(chainGetList(response, "data", "location", "stored"))
                .containsOnly(Map.of("barcode", "ITEM-1"), Map.of("barcode", "ITEM-2"));

        String unstoreMutation = "mutation { unstoreBarcodes(barcodes:[\"ITEM-0\", \"ITEM-1\"]) { numUnstored, unstored { barcode, location { id }}}}";
        response = tester.post(unstoreMutation);
        Map<String, ?> unstoredData = chainGet(response, "data", "unstoreBarcodes");
        assertEquals(1, unstoredData.get("numUnstored"));
        assertThat(chainGetList(unstoredData, "unstored")).containsOnly(Map.of("barcode", "ITEM-1", "location", Map.of("id", li.getId())));

        entityManager.flush();
        refresh(li);
        response = tester.post(getLocationQuery);
        assertThat(chainGetList(response, "data", "location", "stored"))
                .containsOnly(Map.of("barcode", "ITEM-2"));
    }

    @Test
    @Transactional
    public void testStore() throws Exception {
        LocationIdentifier li1 = makeFreezer();
        LocationIdentifier li2 = makeFreezer();
        String toStore = "[{barcode:\"ITEM-1\", address:\"A1\"}, {barcode:\"ITEM-2\", address:\"A2\"}," +
                " {barcode:\"ITEM-3\", location:{barcode:\""+li2.getBarcode()+"\"}}]";
        String storeMutation = tester.readResource("graphql/store.graphql")
                .replace("{id:1}", "{id:"+li1.getId()+"}")
                .replace("[]", toStore);
        Map<String, ?> response = tester.post(storeMutation);
        Map<String, ?> storeData = chainGet(response, "data", "store");
        assertEquals(3, storeData.get("numStored"));
        Map<String, ?> loc1Data = Map.of("id", li1.getId());
        Map<String, ?> loc2Data = Map.of("id", li2.getId());
        Object[] storedItems = {
                storedMap("ITEM-1", loc1Data, "A1"),
                storedMap("ITEM-2", loc1Data, "A2"),
                storedMap("ITEM-3", loc2Data, null)
        };
        assertThat(chainGetList(storeData, "stored")).containsOnly(storedItems);

        refresh(li1);
        String getStoredQuery = tester.readResource("graphql/getstored.graphql")
                .replace("[]", "[\"ITEM-1\",\"ITEM-2\",\"ITEM-3\",\"ITEM-0\"]");
        response = tester.post(getStoredQuery);
        List<Object> storedList = chainGet(response, "data", "stored");
        assertThat(storedList).hasSize(3);
        assertThat(storedList).containsOnly(storedItems);
    }

    @Test
    @Transactional
    public void testEmpty() throws Exception {
        LocationIdentifier li = makeFreezer();
        Object response = tester.post("mutation { storeBarcode(barcode: \"ITEM-1\", location: {id:"+li.getId()+"}) { barcode }}");
        assertEquals("ITEM-1", chainGet(response, "data", "storeBarcode", "barcode"));

        refresh(li);

        String getStoredQuery = tester.readResource("graphql/getstored.graphql")
                .replace("[]", "[\"ITEM-1\"]");
        response = tester.post(getStoredQuery);
        assertEquals(li.getId(), chainGet(response, "data", "stored", 0, "location", "id"));

        response = tester.post("mutation { empty(location:{id:"+li.getId()+"}) { numUnstored, unstored { barcode }}}");
        assertEquals((Integer) 1, chainGet(response, "data", "empty", "numUnstored"));
        assertThat(chainGetList(response, "data", "empty", "unstored")).containsOnly(Map.of("barcode", "ITEM-1"));

        entityManager.flush();
        refresh(li);

        response = tester.post(getStoredQuery);
        assertThat(chainGetList(response, "data", "stored")).isEmpty();

        String getLocationQuery = tester.readResource("graphql/getlocation.graphql")
                .replace("{id:1}", "{id:"+li.getId()+"}");
        response = tester.post(getLocationQuery);
        assertThat(chainGetList(response, "data", "location", "stored")).isEmpty();
    }

    @Test
    @Transactional
    public void testStoreAtAddressAndGetStored() throws Exception {
        // I observed that sometimes the location in the returned item does not include the newly stored item.
        // This test attempts to check that.
        LocationIdentifier li = makeFreezer();
        String mutation = tester.readResource("graphql/storeAtAddressAndGetStored.graphql")
                .replace("LOCATIONBARCODE", li.getBarcode());
        Object response = tester.post(mutation);
        List<Map<String,?>> stored = chainGet(response, "data", "storeBarcode", "location", "stored");
        assertThat(stored).isNotEmpty();
        assertEquals("ITEM-1", chainGet(stored, 0, "barcode"));
        assertEquals("A2", chainGet(stored, 0, "address"));
    }

    @Test
    @Transactional
    public void testStoreMultipleAndGetStored() throws Exception {
        // I observed that sometimes the location in the returned item does not include the newly stored item.
        // This test attempts to check that.
        LocationIdentifier li = makeFreezer();
        String mutation = tester.readResource("graphql/storeMultipleAndGetStored.graphql")
                .replace("LOCATIONBARCODE", li.getBarcode());
        Object response = tester.post(mutation);
        List<Map<String,?>> stored = chainGet(response, "data", "store", "stored");
        assertThat(stored).hasSize(3);
        for (var item : stored) {
            assertThat(chainGetList(item, "location", "stored")).hasSize(3);
        }
    }

    @Test
    @Transactional
    public void testApiKey() throws Exception {
        String mutation = tester.readResource("graphql/addfreezer.graphql");
        Object response = tester.post(mutation, null); // no api key
        List<Map<String, ?>> errors = chainGet(response, "errors");
        assertThat(errors).isNotEmpty();
        String errorMessage = chainGet(errors, 0, "message");
        assertThat(errorMessage).contains("No API key");

        response = tester.post(mutation, "Sw0rdf15h");
        errors = chainGet(response, "errors");
        assertThat(errors).isNotEmpty();
        errorMessage = chainGet(errors, 0, "message");
        assertThat(errorMessage).contains("Invalid API key");

        response = tester.post(mutation);
        errors = chainGet(response, "errors");
        assertThat(errors).isNullOrEmpty();
        assertNotNull(chainGet(response, "data", "addLocation"));
    }

    private static Map<String, Object> storedMap(String barcode, Map<String, ?> location, String address) {
        Map<String, Object> map = new HashMap<>(3);
        map.put("barcode", barcode);
        map.put("location", location);
        map.put("address", address);
        return map;
    }

    private LocationIdentifier makeFreezer() throws Exception {
        String mutation = tester.readResource("graphql/addfreezer.graphql");
        Object response = tester.post(mutation);
        Object locData = chainGet(response, "data", "addLocation");
        Integer id = chainGet(locData, "id");
        String barcode = chainGet(locData, "barcode");
        assertNotNull(id);
        assertThat(barcode).startsWith("STO-");
        LocationIdentifier li = new LocationIdentifier();
        li.setId(id);
        li.setBarcode(barcode);
        return li;
    }

    private void refresh(LocationIdentifier li) {
        entityManager.refresh(db.getLocationRepo().get(li));
    }

    @SuppressWarnings("unchecked")
    private static <T> T chainGet(Object container, Object... accessors) {
        for (int i = 0; i < accessors.length; i++) {
            Object accessor = accessors[i];
            assert container != null;
            Object item;
            if (accessor instanceof Integer) {
                if (!(container instanceof List)) {
                    throw new IllegalArgumentException("["+accessor+"]: container is not list: "+container);
                }
                item = ((List<?>) container).get((int) accessor);
            } else {
                if (!(container instanceof Map)) {
                    throw new IllegalArgumentException("["+accessor+"]: container is not map: "+container);
                }
                item = ((Map<?, ?>) container).get(accessor);
            }
            if (item==null && i < accessors.length-1) {
                throw new IllegalArgumentException("No such element as "+accessor+" in object "+container);
            }
            container = item;
        }
        return (T) container;
    }

    private static <E> List<E> chainGetList(Object container, Object... accessors) {
        return chainGet(container, accessors);
    }
}
