package it.uninsubria.server.service;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import it.uninsubria.server.cache.CacheManager;
import it.uninsubria.server.dao.TestReviewDAO;
import it.uninsubria.shared.model.Review;
import java.util.HashMap;
import java.util.Map;

public class ReviewsServiceCoreTest {

    @Before
    public void setUp() {
        CacheManager.clear();
    }

    @After
    public void tearDown() {
        CacheManager.clear();
    }

    @Test
    public void testSaveReviewDelegation() throws Exception {
        TestReviewDAO dao = new TestReviewDAO();
        ReviewsServiceCore core = new ReviewsServiceCoreImpl(dao);
        Map<String,Integer> scores = new HashMap<>();
        scores.put("Stile", 4);
        Map<String,String> notes = new HashMap<>();
        notes.put("Stile", "buono");
        boolean res = core.saveReview(1, 1, 1,scores, notes);
        assertTrue(res);
        // Should have delegated to addReview once
        assertEquals(1, dao.getAddReviewCalls());
    }

    @Test
    public void testAddAndGet() throws Exception {
        TestReviewDAO dao = new TestReviewDAO();
        ReviewsServiceCore core = new ReviewsServiceCoreImpl(dao);
        Review r = new Review("noteStyle","noteContent","notePleasantness","noteOdness","noteEdition","noteFinal",1,1,1,1,1,1);
        assertTrue(core.addReview(1, 1, r));
        assertEquals(1, dao.getAddReviewCalls());
    }
}
