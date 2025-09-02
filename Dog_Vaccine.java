import java.util.*;

public class DogVaccinationApp {
    // ---------------- Dog Class ----------------
    static class Dog {
        int id;
        boolean infected = false;
        boolean vaccinated = false;
        List<Dog> neighbors = new ArrayList<>();

        Dog(int id) {
            this.id = id;
        }
    }

    // ---------------- DogGraph Class ----------------
    static class DogGraph {
        Map<Integer, Dog> dogs = new HashMap<>();
        Random rand = new Random();

        Dog getDog(int id) {
            return dogs.computeIfAbsent(id, Dog::new);
        }

        void addEdge(int a, int b) {
            Dog da = getDog(a);
            Dog db = getDog(b);
            if (!da.neighbors.contains(db)) da.neighbors.add(db);
            if (!db.neighbors.contains(da)) db.neighbors.add(da);
        }

        void reset() {
            for (Dog d : dogs.values()) {
                d.infected = false;
                d.vaccinated = false;
            }
        }

        void infectRandom(int count) {
            List<Dog> list = new ArrayList<>(dogs.values());
            Collections.shuffle(list, rand);
            for (int i = 0; i < count && i < list.size(); i++) {
                list.get(i).infected = true;
            }
        }

        // ---- Vaccination Strategies ----
        void vaccinateRandom(int count) {
            List<Dog> list = new ArrayList<>(dogs.values());
            Collections.shuffle(list, rand);
            for (int i = 0; i < count && i < list.size(); i++) {
                list.get(i).vaccinated = true;
            }
        }

        void vaccinateHighDegree(int count) {
            List<Dog> list = new ArrayList<>(dogs.values());
            list.sort((a, b) -> b.neighbors.size() - a.neighbors.size());
            for (int i = 0; i < count && i < list.size(); i++) {
                list.get(i).vaccinated = true;
            }
        }

        void vaccinateHighRiskArea(int count) {
            // heuristic: vaccinate neighbors of initially infected
            Set<Dog> targets = new HashSet<>();
            for (Dog d : dogs.values()) {
                if (d.infected) {
                    targets.addAll(d.neighbors);
                }
            }
            List<Dog> list = new ArrayList<>(targets);
            Collections.shuffle(list, rand);

            int i = 0;
            for (; i < count && i < list.size(); i++) {
                list.get(i).vaccinated = true;
            }
            // if still quota left, random others
            if (i < count) {
                List<Dog> rest = new ArrayList<>(dogs.values());
                rest.removeAll(list);
                Collections.shuffle(rest, rand);
                for (int j = 0; i < count && j < rest.size(); i++, j++) {
                    rest.get(j).vaccinated = true;
                }
            }
        }

        // ---- Infection Spread Simulation ----
        int[] simulateSpread() {
            int everInfected = 0;
            Queue<Dog> q = new LinkedList<>();

            for (Dog d : dogs.values()) {
                if (d.infected) {
                    everInfected++;
                    q.add(d);
                }
            }

            while (!q.isEmpty()) {
                Dog cur = q.poll();
                for (Dog nb : cur.neighbors) {
                    if (!nb.infected && !nb.vaccinated) {
                        nb.infected = true;
                        everInfected++;
                        q.add(nb);
                    }
                }
            }

            int finalInfected = 0, vaccinated = 0;
            for (Dog d : dogs.values()) {
                if (d.infected) finalInfected++;
                if (d.vaccinated) vaccinated++;
            }
            return new int[]{everInfected, finalInfected, vaccinated};
        }
    }

    // ---------------- Experiment Class ----------------
    static class Experiment {
        DogGraph baseGraph;
        int runs;
        int initialInfected;
        int vaccines;

        Experiment(DogGraph g, int runs, int initialInfected, int vaccines) {
            this.baseGraph = g;
            this.runs = runs;
            this.initialInfected = initialInfected;
            this.vaccines = vaccines;
        }

        double[] runStrategy(String strategy) {
            DogGraph g = baseGraph;
            g.reset();
            g.infectRandom(initialInfected);

            switch (strategy) {
                case "Random":
                    g.vaccinateRandom(vaccines);
                    break;
                case "HighDegree":
                    g.vaccinateHighDegree(vaccines);
                    break;
                case "HighRiskArea":
                    g.vaccinateHighRiskArea(vaccines);
                    break;
            }
            int[] res = g.simulateSpread();
            return new double[]{res[0], res[1], res[2]};
        }

        public void compareStrategies() {
            String[] strategies = {"Random", "HighDegree", "HighRiskArea"};
            double[][] results = new double[strategies.length][3]; // [strategy][infected, final, vaccinated]

            for (int s = 0; s < strategies.length; s++) {
                String strat = strategies[s];
                double sumEverInfected = 0, sumFinalInfected = 0, sumVaccinated = 0;

                System.out.println("\n=== " + strat + " Strategy Runs ===");

                for (int r = 1; r <= runs; r++) {
                    // run one strategy
                    double[] runRes = runStrategy(strat);

                    // per-run results
                    System.out.printf("Run %d -> EverInfected=%d, FinalInfected=%d, Vaccinated=%d%n",
                            r, (int) runRes[0], (int) runRes[1], (int) runRes[2]);

                    sumEverInfected += runRes[0];
                    sumFinalInfected += runRes[1];
                    sumVaccinated += runRes[2];
                }

                results[s][0] = sumEverInfected / runs;
                results[s][1] = sumFinalInfected / runs;
                results[s][2] = sumVaccinated / runs;
            }

            System.out.println("\n=== RESULTS (averages over runs) ===");
            for (int s = 0; s < strategies.length; s++) {
                System.out.printf("%-12s: avgEverInfected=%.2f, avgFinalInfected=%.2f, avgVaccinated=%.2f%n",
                        strategies[s], results[s][0], results[s][1], results[s][2]);
            }
        }
    }

    // ---------------- Main Method ----------------
    public static void main(String[] args) {
        DogGraph g = new DogGraph();

        // create sample graph of dogs
        int N = 100;
        for (int i = 0; i < N; i++) g.getDog(i);

        Random rand = new Random();
        for (int i = 0; i < N * 2; i++) {
            int a = rand.nextInt(N);
            int b = rand.nextInt(N);
            if (a != b) g.addEdge(a, b);
        }

        Experiment exp = new Experiment(g, 5, 3, 90);
        exp.compareStrategies();
    }
}
