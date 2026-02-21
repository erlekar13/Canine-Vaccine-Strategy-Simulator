import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Unit Tests for DogVaccinationApp
 * ----------------------------------
 * Tests cover:
 *  - Graph construction (Random + Scale-Free)
 *  - Vaccination strategies
 *  - Infection spread logic
 *  - Edge cases
 */
public class DogVaccinationAppTest {

    // =========================================================
    //  Graph Construction Tests
    // =========================================================

    @Test
    @DisplayName("Random graph has correct number of dogs")
    void testRandomGraphNodeCount() {
        DogVaccinationApp.DogGraph g = DogVaccinationApp.DogGraph.buildRandomGraph(50, 0.3);
        assertEquals(50, g.dogs.size(), "Graph should have exactly 50 dogs");
    }

    @Test
    @DisplayName("Scale-Free graph has correct number of dogs")
    void testScaleFreeGraphNodeCount() {
        DogVaccinationApp.DogGraph g = DogVaccinationApp.DogGraph.buildScaleFreeGraph(80, 3);
        assertEquals(80, g.dogs.size(), "Scale-free graph should have exactly 80 dogs");
    }

    @Test
    @DisplayName("Scale-Free graph has at least one high-degree hub node")
    void testScaleFreeGraphHasHub() {
        DogVaccinationApp.DogGraph g = DogVaccinationApp.DogGraph.buildScaleFreeGraph(100, 3);
        // In a scale-free network, max degree should be significantly higher than average
        assertTrue(g.maxDegree() > g.averageDegree() * 2,
                "Scale-free graph should have hub nodes with degree >> average");
    }

    @Test
    @DisplayName("No self-loops in graph")
    void testNoSelfLoops() {
        DogVaccinationApp.DogGraph g = DogVaccinationApp.DogGraph.buildScaleFreeGraph(50, 2);
        for (DogVaccinationApp.Dog d : g.dogs.values()) {
            assertFalse(d.neighbors.contains(d),
                    "Dog#" + d.id + " should not be its own neighbor");
        }
    }

    @Test
    @DisplayName("Edges are undirected (bidirectional)")
    void testUndirectedEdges() {
        DogVaccinationApp.DogGraph g = new DogVaccinationApp.DogGraph();
        g.addEdge(1, 2);
        DogVaccinationApp.Dog d1 = g.getDog(1);
        DogVaccinationApp.Dog d2 = g.getDog(2);
        assertTrue(d1.neighbors.contains(d2), "Dog1 should have Dog2 as neighbor");
        assertTrue(d2.neighbors.contains(d1), "Dog2 should have Dog1 as neighbor");
    }

    // =========================================================
    //  Reset Tests
    // =========================================================

    @Test
    @DisplayName("Reset clears all infected and vaccinated flags")
    void testReset() {
        DogVaccinationApp.DogGraph g = DogVaccinationApp.DogGraph.buildRandomGraph(30, 0.2);
        g.infectRandom(5);
        g.vaccinateRandom(5);
        g.reset();

        for (DogVaccinationApp.Dog d : g.dogs.values()) {
            assertFalse(d.infected,   "After reset, no dog should be infected");
            assertFalse(d.vaccinated, "After reset, no dog should be vaccinated");
        }
    }

    // =========================================================
    //  Vaccination Strategy Tests
    // =========================================================

    @Test
    @DisplayName("Random vaccination vaccinates exactly requested count")
    void testRandomVaccinationCount() {
        DogVaccinationApp.DogGraph g = DogVaccinationApp.DogGraph.buildRandomGraph(50, 0.1);
        g.vaccinateRandom(15);
        long count = g.dogs.values().stream().filter(d -> d.vaccinated).count();
        assertEquals(15, count, "Should vaccinate exactly 15 dogs");
    }

    @Test
    @DisplayName("HighDegree strategy vaccinates the most connected dog first")
    void testHighDegreeVaccinatesMostConnected() {
        DogVaccinationApp.DogGraph g = new DogVaccinationApp.DogGraph();
        // Dog 0 connected to everyone → highest degree
        for (int i = 1; i <= 10; i++) g.addEdge(0, i);
        g.addEdge(1, 2); // dog1 has degree 2

        g.vaccinateHighDegree(1);
        assertTrue(g.getDog(0).vaccinated,
                "Dog with highest degree should be vaccinated first");
    }

    @Test
    @DisplayName("HighRiskArea vaccinates neighbors of infected dogs")
    void testHighRiskAreaTargetsNeighbors() {
        DogVaccinationApp.DogGraph g = new DogVaccinationApp.DogGraph();
        // Dog 0 infected, dogs 1,2,3 are its neighbors
        g.addEdge(0, 1);
        g.addEdge(0, 2);
        g.addEdge(0, 3);
        g.addEdge(4, 5); // unrelated edge
        g.getDog(0).infected = true;

        g.vaccinateHighRiskArea(3);

        assertTrue(g.getDog(1).vaccinated, "Dog1 (neighbor of infected) should be vaccinated");
        assertTrue(g.getDog(2).vaccinated, "Dog2 (neighbor of infected) should be vaccinated");
        assertTrue(g.getDog(3).vaccinated, "Dog3 (neighbor of infected) should be vaccinated");
    }

    @Test
    @DisplayName("Cannot vaccinate more dogs than exist")
    void testVaccinationDoesNotExceedPopulation() {
        DogVaccinationApp.DogGraph g = DogVaccinationApp.DogGraph.buildRandomGraph(20, 0.2);
        g.vaccinateRandom(9999); // request more than population
        long count = g.dogs.values().stream().filter(d -> d.vaccinated).count();
        assertEquals(20, count, "Cannot vaccinate more than total population");
    }

    // =========================================================
    //  Infection Spread Tests
    // =========================================================

    @Test
    @DisplayName("Vaccinated dogs do not get infected")
    void testVaccinatedDogsNotInfected() {
        DogVaccinationApp.DogGraph g = new DogVaccinationApp.DogGraph();
        // Linear chain: 0 → 1 → 2 → 3
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);

        g.getDog(0).infected   = true;
        g.getDog(1).vaccinated = true;  // block spread at dog1
        g.getDog(2).vaccinated = true;

        g.simulateSpread();

        assertFalse(g.getDog(3).infected,
                "Dog3 should not be infected when blocked by vaccinated dogs");
    }

    @Test
    @DisplayName("Isolated dogs (no edges) don't spread infection")
    void testIsolatedDogNoSpread() {
        DogVaccinationApp.DogGraph g = new DogVaccinationApp.DogGraph();
        g.getDog(0).infected = true; // isolated — no neighbors
        g.getDog(1);                 // isolated — no neighbors

        int[] res = g.simulateSpread();
        assertEquals(1, res[1], "Only initially infected dog should be infected");
    }

    @Test
    @DisplayName("simulateSpread returns non-negative counts")
    void testSimulateSpreadReturnsValid() {
        DogVaccinationApp.DogGraph g = DogVaccinationApp.DogGraph.buildScaleFreeGraph(50, 2);
        g.infectRandom(3);
        g.vaccinateRandom(10);

        int[] res = g.simulateSpread();
        assertTrue(res[0] >= 0, "everInfected must be >= 0");
        assertTrue(res[1] >= 0, "finalInfected must be >= 0");
        assertTrue(res[2] >= 0, "vaccinated count must be >= 0");
        assertTrue(res[1] <= g.dogs.size(), "finalInfected cannot exceed population");
    }

    // =========================================================
    //  Experiment / Multi-Run Tests
    // =========================================================

    @Test
    @DisplayName("Each run is statistically independent (results can differ)")
    void testRunsAreIndependent() {
        DogVaccinationApp.DogGraph g = DogVaccinationApp.DogGraph.buildScaleFreeGraph(100, 3);
        DogVaccinationApp.Experiment exp = new DogVaccinationApp.Experiment(g, 5, 5, 20);

        // Collect results of 5 independent runs
        Set<Integer> uniqueResults = new HashSet<>();
        for (int r = 1; r <= 5; r++) {
            DogVaccinationApp.SimulationResult res = exp.runOnce("Random", r);
            uniqueResults.add(res.finalInfected);
        }
        // With probabilistic spread, results across runs should not all be identical
        assertTrue(uniqueResults.size() > 1,
                "Independent runs with probabilistic spread should produce varied results");
    }

    @Test
    @DisplayName("HighDegree strategy performs better than Random on scale-free graph")
    void testHighDegreeBeatsRandomOnScaleFree() {
        // Run many times and check average
        DogVaccinationApp.DogGraph g = DogVaccinationApp.DogGraph.buildScaleFreeGraph(100, 3);
        int TRIALS = 20;
        double sumRandom = 0, sumHighDeg = 0;

        DogVaccinationApp.Experiment exp = new DogVaccinationApp.Experiment(g, TRIALS, 3, 20);

        for (int r = 1; r <= TRIALS; r++) {
            sumRandom  += exp.runOnce("Random",     r).finalInfected;
            sumHighDeg += exp.runOnce("HighDegree", r).finalInfected;
        }

        double avgRandom  = sumRandom  / TRIALS;
        double avgHighDeg = sumHighDeg / TRIALS;

        assertTrue(avgHighDeg <= avgRandom,
                String.format(
                    "HighDegree (%.2f) should infect fewer dogs than Random (%.2f) on scale-free graphs",
                    avgHighDeg, avgRandom));
    }
}
