package com.smartpantry.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemControllerTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        String dbFile = tempDir.toAbsolutePath().toString().replace('\\', '/') + "/smartpantry";
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:" + dbFile);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ItemRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    /** Creates an item through POST and returns its id from the response. */
    private long createAndGetId(String body) throws Exception {
        String response = createItem(body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private static final long UNKNOWN_ID = 999999999L;

    private static void assertStandardNotFoundBody(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("Exception"), body);
        assertFalse(body.contains("SELECT"), body);
        assertFalse(body.contains("at com."), body);
    }

    private static String validBody(String name) {
        return json(str(name), "2", str("Fruits"), str(LocalDate.now().minusDays(1).toString()),
                str(LocalDate.now().plusDays(10).toString()));
    }

    private static String json(String name, String quantity, String category,
                               String purchaseDate, String expiryDate) {
        return "{"
                + "\"name\":" + name + ","
                + "\"quantity\":" + quantity + ","
                + "\"category\":" + category + ","
                + "\"purchaseDate\":" + purchaseDate + ","
                + "\"expiryDate\":" + expiryDate
                + "}";
    }

    private static String str(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private ResultActions createItem(String body) throws Exception {
        return mvc.perform(post("/api/items").contentType("application/json").content(body));
    }

    private static String today() {
        return LocalDate.now().toString();
    }

    @Test
    void validCreateReturns201WithSavedItem() throws Exception {
        String purchase = LocalDate.now().minusDays(1).toString();
        String expiry = LocalDate.now().plusDays(10).toString();

        createItem(json(str("Apple"), "3", str("Fruits"), str(purchase), str(expiry)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Apple"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.category").value("Fruits"))
                .andExpect(jsonPath("$.purchaseDate").value(purchase))
                .andExpect(jsonPath("$.expiryDate").value(expiry))
                .andExpect(jsonPath("$.status").value("valid"));
    }

    @Test
    void statusIsNoneWhenExpiryIsOmittedAndExpiringWhenExpiryIsToday() throws Exception {
        createItem("{\"name\":\"Rice\",\"quantity\":1,\"category\":\"Grains\",\"purchaseDate\":\"" + today() + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiryDate").doesNotExist())
                .andExpect(jsonPath("$.status").value("none"));

        createItem(json(str("Milk"), "1", str("Dairy"), str(today()), str(today())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("expiring"));
    }

    @Test
    void expiredItemIsReportedAsExpired() throws Exception {
        String yesterday = LocalDate.now().minusDays(1).toString();
        createItem(json(str("Yogurt"), "1", str("Dairy"), str(yesterday), str(yesterday)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("expired"));
    }

    @Test
    void invalidFieldsReturn400WithEveryFailingField() throws Exception {
        long before = repository.count();

        // Every value here has the right JSON type, so all three rule checks run.
        createItem(json(str(" "), "0", str("Dairy"), str(LocalDate.now().plusDays(1).toString()), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists())
                .andExpect(jsonPath("$.fieldErrors.purchaseDate").exists());

        assertEquals(before, repository.count());
    }

    @Test
    void missingNameReturns400OnName() throws Exception {
        createItem(json("null", "1", str("Dairy"), str(today()), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void quantityDecimalOrTextReturns400OnQuantity() throws Exception {
        createItem(json(str("Milk"), "1.5", str("Dairy"), str(today()), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());

        createItem(json(str("Milk"), str("abc"), str("Dairy"), str(today()), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void invalidCategoryReturns400OnCategory() throws Exception {
        createItem(json(str("Steak"), "1", str("Meat"), str(today()), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.category").exists());
    }

    @Test
    void invalidDateTextReturns400OnThatDate() throws Exception {
        createItem(json(str("Milk"), "1", str("Dairy"), str("2026-02-30"), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.purchaseDate").exists());

        createItem(json(str("Milk"), "1", str("Dairy"), str(today()), str("25/12/2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.expiryDate").exists());
    }

    @Test
    void purchaseAfterExpiryAndFuturePurchaseReturn400OnPurchaseDate() throws Exception {
        createItem(json(str("Milk"), "1", str("Dairy"), str(today()), str(LocalDate.now().minusDays(1).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.purchaseDate").exists());

        createItem(json(str("Milk"), "1", str("Dairy"), str(LocalDate.now().plusDays(1).toString()), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.purchaseDate").exists());
    }

    @Test
    void malformedJsonReturns400WithoutInternals() throws Exception {
        createItem("{\"name\": \"Milk\", \"quantity\": ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertFalse(body.contains("Exception"), body);
                    assertFalse(body.contains("at com."), body);
                });
    }

    @Test
    void emptyBodyReturns400WithReadableMessage() throws Exception {
        createItem("")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void listReturnsItemsOrderedByNameThenIdWithStatus() throws Exception {
        createItem(json(str("Milk"), "1", str("Dairy"), str(today()), str(LocalDate.now().plusDays(30).toString())))
                .andExpect(status().isCreated());
        createItem(json(str("Apple"), "2", str("Fruits"), str(today()), null))
                .andExpect(status().isCreated());
        createItem(json(str("Apple"), "5", str("Fruits"), str(LocalDate.now().minusDays(5).toString()), str(LocalDate.now().minusDays(2).toString())))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Apple"))
                .andExpect(jsonPath("$[0].status").value("none"))
                .andExpect(jsonPath("$[1].name").value("Apple"))
                .andExpect(jsonPath("$[1].status").value("expired"))
                .andExpect(jsonPath("$[2].name").value("Milk"))
                .andExpect(jsonPath("$[2].status").value("valid"));
    }

    @Test
    void listIsEmptyWhenNothingIsStored() throws Exception {
        mvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getByIdReturns200WithTheItem() throws Exception {
        long id = createAndGetId(validBody("Apple"));

        mvc.perform(get("/api/items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Apple"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.category").value("Fruits"))
                .andExpect(jsonPath("$.status").value("valid"));
    }

    @Test
    void getByUnknownIdReturns404WithStandardBody() throws Exception {
        mvc.perform(get("/api/items/" + UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(ItemControllerTest::assertStandardNotFoundBody);
    }

    @Test
    void putUpdatesTheItemAndReturns200() throws Exception {
        long id = createAndGetId(validBody("Apple"));
        String update = json(str("Pear"), "5", str("Dairy"), str(today()), "null");

        mvc.perform(put("/api/items/" + id).contentType("application/json").content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Pear"))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.category").value("Dairy"))
                .andExpect(jsonPath("$.purchaseDate").value(today()))
                .andExpect(jsonPath("$.expiryDate").doesNotExist())
                .andExpect(jsonPath("$.status").value("none"));

        mvc.perform(get("/api/items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pear"));
    }

    @Test
    void putToUnknownIdReturns404WithStandardBody() throws Exception {
        mvc.perform(put("/api/items/" + UNKNOWN_ID).contentType("application/json")
                        .content(validBody("Pear")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(ItemControllerTest::assertStandardNotFoundBody);
    }

    @Test
    void putWithInvalidFieldsReturns400WithFieldErrorsAndLeavesItemUnchanged() throws Exception {
        long id = createAndGetId(validBody("Apple"));
        String invalid = json(str(" "), "0", str("Dairy"), str(today()), "null");

        mvc.perform(put("/api/items/" + id).contentType("application/json").content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());

        mvc.perform(get("/api/items/" + id))
                .andExpect(jsonPath("$.name").value("Apple"))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void putWithDecimalQuantityReturns400OnQuantity() throws Exception {
        long id = createAndGetId(validBody("Apple"));
        String decimal = json(str("Apple"), "1.5", str("Fruits"), str(today()), "null");

        mvc.perform(put("/api/items/" + id).contentType("application/json").content(decimal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void deleteReturns204AndTheItemIsGoneFromGetAndList() throws Exception {
        long keep = createAndGetId(validBody("Milk"));
        long id = createAndGetId(validBody("Apple"));

        mvc.perform(delete("/api/items/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/items/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isString());

        mvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty())
                .andExpect(jsonPath("$[0].id").value(keep));
    }

    @Test
    void deleteUnknownIdReturns404WithStandardBody() throws Exception {
        mvc.perform(delete("/api/items/" + UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(ItemControllerTest::assertStandardNotFoundBody);
    }
}
