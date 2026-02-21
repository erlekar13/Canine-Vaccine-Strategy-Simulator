import javafx.animation.*;
import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.util.Duration;

import java.io.*;
import java.util.*;

/**
 * ╔══════════════════════════════════════════════════════════╗
 *  CANINE VACCINE STRATEGY SIMULATOR v3.0
 *  ─────────────────────────────────────────────────────────
 *  ONE FILE — contains everything:
 *    1. Simulation Engine  (Dog, DogGraph, Experiment)
 *    2. JavaFX Visual GUI  (Graph + Bar Chart + Line Chart)
 *    3. CSV Export
 *
 *  HOW TO COMPILE & RUN:
 *    javac --module-path /path/to/javafx/lib \
 *          --add-modules javafx.controls \
 *          DogVaccinationApp.java
 *
 *    java  --module-path /path/to/javafx/lib \
 *          --add-modules javafx.controls \
 *          DogVaccinationApp
 * ╚══════════════════════════════════════════════════════════╝
 */
public class DogVaccinationApp extends Application {

    // ══════════════════════════════════════════════════════
    //  CONSTANTS
    // ══════════════════════════════════════════════════════
    static final int    N_DOGS         = 100;
    static final int    EDGES_PER_NODE = 3;
    static final int    INIT_INFECTED  = 5;
    static final int    VACCINES       = 30;
    static final int    RUNS           = 10;
    static final double INFECTION_PROB = 0.40;

    // ══════════════════════════════════════════════════════
    //  COLOR PALETTE
    // ══════════════════════════════════════════════════════
    static final Color BG         = Color.web("#0d1117");
    static final Color PANEL      = Color.web("#161b22");
    static final Color HEALTHY    = Color.web("#2ecc71");
    static final Color INFECTED   = Color.web("#e74c3c");
    static final Color VACCINATED = Color.web("#3498db");
    static final Color ACCENT     = Color.web("#f39c12");
    static final Color TEXT       = Color.web("#e6edf3");
    static final Color MUTED      = Color.web("#8b949e");
    static final Color EDGE_COL   = Color.web("#ffffff", 0.07);

    // ══════════════════════════════════════════════════════
    //  INNER CLASS — Dog (Graph Node)
    // ══════════════════════════════════════════════════════
    static class Dog {
        int       id;
        boolean   infected   = false;
        boolean   vaccinated = false;
        List<Dog> neighbors  = new ArrayList<>();

        Dog(int id) { this.id = id; }
        int degree() { return neighbors.size(); }
    }

    // ══════════════════════════════════════════════════════
    //  INNER CLASS — DogGraph
    // ══════════════════════════════════════════════════════
    static class DogGraph {
        Map<Integer, Dog> dogs = new LinkedHashMap<>();
        Random rand = new Random();

        Dog getDog(int id) {
            return dogs.computeIfAbsent(id, Dog::new);
        }

        void addEdge(int a, int b) {
            if (a == b) return;
            Dog da = getDog(a), db = getDog(b);
            if (!da.neighbors.contains(db)) {
                da.neighbors.add(db);
                db.neighbors.add(da);
            }
        }

        // ── Barabasi-Albert Scale-Free Graph ──────────────
        static DogGraph buildScaleFreeGraph(int n, int edgesPerNode) {
            DogGraph g    = new DogGraph();
            Random   rand = new Random();
            int      core = Math.min(edgesPerNode + 1, n);

            for (int i = 0; i < core; i++) g.getDog(i);
            for (int i = 0; i < core; i++)
                for (int j = i+1; j < core; j++)
                    g.addEdge(i, j);

            for (int newId = core; newId < n; newId++) {
                g.getDog(newId);
                List<Dog> pool = new ArrayList<>();
                for (Dog d : g.dogs.values()) {
                    if (d.id == newId) continue;
                    for (int k = 0; k < Math.max(1, d.degree()); k++) pool.add(d);
                }
                Collections.shuffle(pool, rand);
                Set<Integer> chosen = new HashSet<>();
                for (Dog target : pool) {
                    if (chosen.size() >= edgesPerNode) break;
                    if (chosen.add(target.id)) g.addEdge(newId, target.id);
                }
            }
            return g;
        }

        void reset() {
            dogs.values().forEach(d -> { d.infected = false; d.vaccinated = false; });
        }

        void infectRandom(int count) {
            List<Dog> list = new ArrayList<>(dogs.values());
            Collections.shuffle(list, rand);
            for (int i = 0; i < count && i < list.size(); i++)
                list.get(i).infected = true;
        }

        // ── Strategy 1: Random ────────────────────────────
        void vaccinateRandom(int count) {
            List<Dog> list = new ArrayList<>(dogs.values());
            Collections.shuffle(list, rand);
            for (int i = 0; i < count && i < list.size(); i++)
                list.get(i).vaccinated = true;
        }

        // ── Strategy 2: HighDegree ────────────────────────
        void vaccinateHighDegree(int count) {
            List<Dog> list = new ArrayList<>(dogs.values());
            list.sort((a, b) -> b.degree() - a.degree());
            for (int i = 0; i < count && i < list.size(); i++)
                list.get(i).vaccinated = true;
        }

        // ── Strategy 3: HighRiskArea ──────────────────────
        void vaccinateHighRiskArea(int count) {
            Set<Dog> targets = new LinkedHashSet<>();
            dogs.values().stream().filter(d -> d.infected)
                .forEach(d -> targets.addAll(d.neighbors));
            List<Dog> list = new ArrayList<>(targets);
            Collections.shuffle(list, rand);
            int i = 0;
            for (; i < count && i < list.size(); i++) list.get(i).vaccinated = true;
            if (i < count) {
                List<Dog> rest = new ArrayList<>(dogs.values());
                rest.removeAll(list);
                Collections.shuffle(rest, rand);
                for (int j = 0; i < count && j < rest.size(); i++, j++)
                    rest.get(j).vaccinated = true;
            }
        }

        // ── Probabilistic BFS Spread (SI Model) ──────────
        int[] simulateSpread() {
            Random     r     = new Random();
            Queue<Dog> queue = new LinkedList<>();
            int        ever  = 0;
            for (Dog d : dogs.values()) if (d.infected) { ever++; queue.add(d); }
            while (!queue.isEmpty()) {
                Dog cur = queue.poll();
                for (Dog nb : cur.neighbors) {
                    if (!nb.infected && !nb.vaccinated && r.nextDouble() < INFECTION_PROB) {
                        nb.infected = true; ever++; queue.add(nb);
                    }
                }
            }
            int fin = 0, vacc = 0;
            for (Dog d : dogs.values()) { if (d.infected) fin++; if (d.vaccinated) vacc++; }
            return new int[]{ ever, fin, vacc };
        }

        // ── BFS Waves for Animation ───────────────────────
        List<List<Integer>> getWaves() {
            Random              r       = new Random();
            Queue<Integer>      q       = new LinkedList<>();
            boolean[]           visited = new boolean[dogs.size()];
            List<List<Integer>> waves   = new ArrayList<>();
            dogs.values().stream().filter(d -> d.infected)
                .forEach(d -> { q.add(d.id); visited[d.id] = true; });
            while (!q.isEmpty()) {
                List<Integer> wave = new ArrayList<>();
                int sz = q.size();
                for (int k = 0; k < sz; k++) {
                    Dog cur = dogs.get(q.poll());
                    for (Dog nb : cur.neighbors) {
                        if (!visited[nb.id] && !nb.vaccinated && r.nextDouble() < INFECTION_PROB) {
                            visited[nb.id] = true; nb.infected = true;
                            wave.add(nb.id); q.add(nb.id);
                        }
                    }
                }
                if (!wave.isEmpty()) waves.add(wave);
            }
            return waves;
        }

        double averageDegree() {
            return dogs.values().stream().mapToInt(Dog::degree).average().orElse(0);
        }
        int maxDegree() {
            return dogs.values().stream().mapToInt(Dog::degree).max().orElse(0);
        }
    }

    // ══════════════════════════════════════════════════════
    //  INNER CLASS — SimulationResult
    // ══════════════════════════════════════════════════════
    static class SimulationResult {
        String strategy; int run, everInfected, finalInfected, vaccinated, total;
        SimulationResult(String s, int r, int[] res, int t) {
            strategy=s; run=r; everInfected=res[0];
            finalInfected=res[1]; vaccinated=res[2]; total=t;
        }
        double infectionRate() { return finalInfected*100.0/total; }
    }

    // ══════════════════════════════════════════════════════
    //  INNER CLASS — Experiment
    // ══════════════════════════════════════════════════════
    static class Experiment {
        DogGraph graph; int runs, initInfected, vaccines;
        List<SimulationResult> allResults = new ArrayList<>();

        Experiment(DogGraph g, int runs, int init, int vacc) {
            graph=g; this.runs=runs; initInfected=init; vaccines=vacc;
        }

        SimulationResult runOnce(String strategy, int runNum) {
            graph.reset();
            graph.infectRandom(initInfected);
            switch (strategy) {
                case "Random":       graph.vaccinateRandom(vaccines);       break;
                case "HighDegree":   graph.vaccinateHighDegree(vaccines);   break;
                case "HighRiskArea": graph.vaccinateHighRiskArea(vaccines);  break;
            }
            int[] res = graph.simulateSpread();
            SimulationResult sr = new SimulationResult(strategy, runNum, res, graph.dogs.size());
            allResults.add(sr);
            return sr;
        }

        double[][] compareStrategies() {
            String[]   strats = {"Random","HighDegree","HighRiskArea"};
            double[][] avg    = new double[3][3];

            System.out.println("\n" + "=".repeat(65));
            System.out.println("   CANINE VACCINE STRATEGY SIMULATOR v3.0");
            System.out.println("=".repeat(65));
            System.out.printf("  Dogs: %d | Infected: %d | Vaccines: %d | Runs: %d%n",
                    graph.dogs.size(), initInfected, vaccines, runs);
            System.out.printf("  Inf Prob: %.0f%% | Avg Degree: %.2f | Max Degree: %d%n",
                    INFECTION_PROB*100, graph.averageDegree(), graph.maxDegree());
            System.out.println("=".repeat(65));

            for (int s = 0; s < 3; s++) {
                double sumE=0, sumF=0, sumV=0;
                System.out.printf("%n>>> Strategy: %-12s <<<%n", strats[s]);
                System.out.printf("%-6s %-14s %-16s %-12s %-10s%n",
                        "Run","EverInfected","FinalInfected","Vaccinated","InfRate%");
                System.out.println("-".repeat(60));
                for (int r = 1; r <= runs; r++) {
                    SimulationResult res = runOnce(strats[s], r);
                    System.out.printf("%-6d %-14d %-16d %-12d %.2f%%%n",
                            r, res.everInfected, res.finalInfected,
                            res.vaccinated, res.infectionRate());
                    sumE+=res.everInfected; sumF+=res.finalInfected; sumV+=res.vaccinated;
                }
                avg[s][0]=sumE/runs; avg[s][1]=sumF/runs; avg[s][2]=sumV/runs;
            }

            System.out.println("\n" + "=".repeat(65));
            System.out.println("   SUMMARY");
            System.out.printf("%-14s %-18s %-18s %-12s%n",
                    "Strategy","AvgEverInfected","AvgFinalInfected","AvgVaccinated");
            System.out.println("-".repeat(65));
            int best = 0;
            for (int s = 0; s < 3; s++) {
                System.out.printf("%-14s %-18.2f %-18.2f %-12.2f%n",
                        strats[s], avg[s][0], avg[s][1], avg[s][2]);
                if (avg[s][1] < avg[best][1]) best = s;
            }
            System.out.printf("%n  Best: %s | Improvement: %.1f%% over Random%n",
                    strats[best], (avg[0][1]-avg[best][1])/avg[0][1]*100);
            System.out.println("=".repeat(65));

            try { exportCSV("simulation_results.csv", strats, avg); }
            catch (IOException e) { System.out.println("CSV export failed: "+e.getMessage()); }
            return avg;
        }

        void exportCSV(String file, String[] strats, double[][] avg) throws IOException {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("Strategy,AvgEverInfected,AvgFinalInfected,AvgVaccinated,InfRate%");
                for (int s = 0; s < 3; s++)
                    pw.printf("%s,%.2f,%.2f,%.2f,%.2f%n", strats[s],
                            avg[s][0], avg[s][1], avg[s][2],
                            avg[s][1]/graph.dogs.size()*100);
                pw.println("\nStrategy,Run,EverInfected,FinalInfected,Vaccinated,InfRate%");
                for (SimulationResult r : allResults)
                    pw.printf("%s,%d,%d,%d,%d,%.2f%n", r.strategy, r.run,
                            r.everInfected, r.finalInfected, r.vaccinated, r.infectionRate());
            }
            System.out.println("  Exported → " + file);
        }
    }

    // ══════════════════════════════════════════════════════
    //  JAVAFX STATE
    // ══════════════════════════════════════════════════════
    DogGraph   graph;
    Experiment experiment;
    double[]   nodeX, nodeY;
    double[][] avgResults  = new double[3][3];
    double[][] perRunFinal = new double[3][RUNS];
    String[]   strategies  = {"Random","HighDegree","HighRiskArea"};
    String     curStrat    = "Random";
    boolean    animating   = false;

    Canvas      graphCanvas, barCanvas, lineCanvas;
    Label       statusLabel, stratLabel;
    Button      runBtn;
    ProgressBar progressBar;

    // ══════════════════════════════════════════════════════
    //  JAVAFX start()
    // ══════════════════════════════════════════════════════
    @Override
    public void start(Stage stage) {
        graph      = DogGraph.buildScaleFreeGraph(N_DOGS, EDGES_PER_NODE);
        experiment = new Experiment(graph, RUNS, INIT_INFECTED, VACCINES);
        layoutNodes();
        precomputeResults();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0d1117;");
        root.setTop(buildTopBar());
        root.setCenter(buildCenter());
        root.setBottom(buildControls());

        stage.setScene(new Scene(root, 1200, 730));
        stage.setTitle("🐕 Canine Vaccine Strategy Simulator v3.0");
        stage.show();

        drawGraph(curStrat);
        drawBarChart();
        drawLineChart();
    }

    // ── Top Bar ───────────────────────────────────────────
    HBox buildTopBar() {
        Label title = new Label("🐕  CANINE VACCINE STRATEGY SIMULATOR  v3.0");
        title.setFont(Font.font("Courier New", FontWeight.BOLD, 18));
        title.setTextFill(ACCENT);

        Label meta = new Label(
            "Dogs: "+N_DOGS+" | Infected: "+INIT_INFECTED+" | Vaccines: "+VACCINES+
            " | Runs: "+RUNS+" | Inf Prob: "+(int)(INFECTION_PROB*100)+"%"+
            " | Avg Degree: "+String.format("%.2f",graph.averageDegree())+
            " | Max Degree: "+graph.maxDegree());
        meta.setFont(Font.font("Courier New", 11));
        meta.setTextFill(MUTED);

        stratLabel = new Label("Strategy: Random");
        stratLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
        stratLabel.setTextFill(HEALTHY);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(16, new VBox(4, title, meta), sp, stratLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14,22,14,22));
        bar.setStyle("-fx-background-color:#161b22;-fx-border-color:#30363d;-fx-border-width:0 0 1 0;");
        return bar;
    }

    // ── Center ────────────────────────────────────────────
    HBox buildCenter() {
        graphCanvas = new Canvas(650, 490);
        StackPane graphPane = new StackPane(graphCanvas);
        graphPane.setStyle("-fx-background-color:#161b22;-fx-background-radius:10;");
        graphPane.setPadding(new Insets(8));

        barCanvas  = new Canvas(460, 200);
        lineCanvas = new Canvas(460, 185);

        VBox charts = new VBox(12,
            chartBox("Avg Final Infected — Strategy Comparison", barCanvas),
            chartBox("Infection per Run — All 3 Strategies",     lineCanvas),
            buildLegend(),
            buildResultTable()
        );
        charts.setPrefWidth(500);

        HBox center = new HBox(14, graphPane, charts);
        center.setPadding(new Insets(12,16,0,16));
        HBox.setHgrow(graphPane, Priority.ALWAYS);
        return center;
    }

    VBox chartBox(String title, Canvas c) {
        Label l = new Label(title);
        l.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        l.setTextFill(MUTED);
        StackPane w = new StackPane(c);
        w.setStyle("-fx-background-color:#161b22;-fx-background-radius:8;");
        return new VBox(5, l, w);
    }

    HBox buildLegend() {
        HBox h = new HBox(18, dot(HEALTHY,"Healthy"), dot(INFECTED,"Infected"), dot(VACCINATED,"Vaccinated"));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    HBox dot(Color c, String label) {
        javafx.scene.shape.Circle d = new javafx.scene.shape.Circle(6, c);
        Label l = new Label(label);
        l.setFont(Font.font("Courier New", 11)); l.setTextFill(TEXT);
        HBox b = new HBox(6, d, l); b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    GridPane buildResultTable() {
        GridPane g = new GridPane();
        g.setHgap(20); g.setVgap(4);
        g.setPadding(new Insets(6,0,0,0));

        String[] headers = {"Strategy","Avg Infected","Inf Rate","Reduction"};
        for (int c = 0; c < 4; c++) {
            Label h = new Label(headers[c]);
            h.setFont(Font.font("Courier New",10)); h.setTextFill(MUTED);
            g.add(h, c, 0);
        }

        String[] strats  = {"Random","HighDegree","HighRiskArea"};
        double   baseline = avgResults[0][1];
        int      best     = 0;
        for (int s=1; s<3; s++) if (avgResults[s][1]<avgResults[best][1]) best=s;

        for (int s = 0; s < 3; s++) {
            double val = avgResults[s][1];
            String red = s==0 ? "—" : String.format("%.1f%%",(baseline-val)/baseline*100);
            Color  col = s==best ? HEALTHY : TEXT;
            g.addRow(s+1,
                cell((s==best?"★ ":"")+strats[s], col),
                cell(String.format("%.1f",val), col),
                cell(String.format("%.1f%%",val/N_DOGS*100), col),
                cell(red, s==0?MUTED:HEALTHY));
        }
        return g;
    }

    Label cell(String t, Color c) {
        Label l = new Label(t);
        l.setFont(Font.font("Courier New",11)); l.setTextFill(c); return l;
    }

    // ── Controls ──────────────────────────────────────────
    HBox buildControls() {
        ToggleGroup tg    = new ToggleGroup();
        Color[]     cols  = {HEALTHY, VACCINATED, ACCENT};
        HBox        btns  = new HBox(8);

        for (int i = 0; i < 3; i++) {
            String s=strategies[i]; Color col=cols[i];
            ToggleButton tb = new ToggleButton(s);
            tb.setToggleGroup(tg);
            tb.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
            String hex = toHex(col);
            tb.setStyle("-fx-background-color:#21262d;-fx-text-fill:"+hex+
                ";-fx-border-color:"+hex+";-fx-border-radius:6;-fx-background-radius:6;-fx-padding:6 16;");
            if (i==0) tb.setSelected(true);
            tb.setOnAction(e -> {
                curStrat=s; stratLabel.setText("Strategy: "+s); stratLabel.setTextFill(col);
                drawGraph(s);
            });
            btns.getChildren().add(tb);
        }

        runBtn = new Button("▶  Run Animation");
        runBtn.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
        runBtn.setStyle("-fx-background-color:#2ecc71;-fx-text-fill:#0d1117;-fx-background-radius:6;-fx-padding:8 20;");
        runBtn.setOnAction(e -> animateSpread());

        Button resetBtn = new Button("↺  Reset");
        resetBtn.setFont(Font.font("Courier New",12));
        resetBtn.setStyle("-fx-background-color:#21262d;-fx-text-fill:#e6edf3;-fx-border-color:#30363d;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 16;");
        resetBtn.setOnAction(e -> {
            graph=DogGraph.buildScaleFreeGraph(N_DOGS,EDGES_PER_NODE);
            experiment=new Experiment(graph,RUNS,INIT_INFECTED,VACCINES);
            layoutNodes(); precomputeResults();
            drawGraph(curStrat); drawBarChart(); drawLineChart();
            progressBar.setProgress(0);
            statusLabel.setText("Graph rebuilt with new random seed");
        });

        statusLabel = new Label("Select a strategy and click Run Animation");
        statusLabel.setFont(Font.font("Courier New",11)); statusLabel.setTextFill(MUTED);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(180);
        progressBar.setStyle("-fx-accent:#2ecc71;");

        Region sp = new Region(); HBox.setHgrow(sp,Priority.ALWAYS);
        HBox bar = new HBox(10,btns,runBtn,resetBtn,sp,statusLabel,progressBar);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12,20,14,20));
        bar.setStyle("-fx-background-color:#161b22;-fx-border-color:#30363d;-fx-border-width:1 0 0 0;");
        return bar;
    }

    // ══════════════════════════════════════════════════════
    //  NODE LAYOUT
    // ══════════════════════════════════════════════════════
    void layoutNodes() {
        nodeX=new double[N_DOGS]; nodeY=new double[N_DOGS];
        double cx=325,cy=245,r=200; Random rand=new Random();
        for (int i=0;i<N_DOGS;i++) {
            double a=2*Math.PI*i/N_DOGS;
            double d=r*(0.65+rand.nextDouble()*0.35);
            nodeX[i]=cx+d*Math.cos(a); nodeY[i]=cy+d*Math.sin(a);
        }
    }

    // ══════════════════════════════════════════════════════
    //  PRECOMPUTE CHART DATA
    // ══════════════════════════════════════════════════════
    void precomputeResults() {
        for (int s=0;s<3;s++) {
            double sum=0;
            for (int r=0;r<RUNS;r++) {
                SimulationResult res=experiment.runOnce(strategies[s],r+1);
                perRunFinal[s][r]=res.finalInfected; sum+=res.finalInfected;
            }
            avgResults[s][1]=sum/RUNS;
        }
    }

    // ══════════════════════════════════════════════════════
    //  DRAW — Graph
    // ══════════════════════════════════════════════════════
    void drawGraph(String strategy) {
        GraphicsContext gc=graphCanvas.getGraphicsContext2D();
        double W=graphCanvas.getWidth(),H=graphCanvas.getHeight();
        gc.setFill(PANEL); gc.fillRoundRect(0,0,W,H,12,12);

        graph.reset(); graph.infectRandom(INIT_INFECTED);
        switch(strategy) {
            case "Random":       graph.vaccinateRandom(VACCINES);      break;
            case "HighDegree":   graph.vaccinateHighDegree(VACCINES);  break;
            case "HighRiskArea": graph.vaccinateHighRiskArea(VACCINES); break;
        }
        graph.simulateSpread();

        // Edges
        gc.setStroke(EDGE_COL); gc.setLineWidth(0.8);
        graph.dogs.values().forEach(d -> d.neighbors.forEach(nb -> {
            if (d.id<nb.id) gc.strokeLine(nodeX[d.id],nodeY[d.id],nodeX[nb.id],nodeY[nb.id]);
        }));

        // Nodes
        graph.dogs.values().forEach(d -> {
            Color  c = d.vaccinated?VACCINATED:d.infected?INFECTED:HEALTHY;
            double sz= 4.5+Math.min(d.degree()*0.55,9);
            if (d.degree()>8) {
                gc.setFill(Color.color(c.getRed(),c.getGreen(),c.getBlue(),0.14));
                gc.fillOval(nodeX[d.id]-sz*2.5,nodeY[d.id]-sz*2.5,sz*5,sz*5);
            }
            gc.setFill(c); gc.fillOval(nodeX[d.id]-sz/2,nodeY[d.id]-sz/2,sz,sz);
            gc.setStroke(Color.color(c.getRed(),c.getGreen(),c.getBlue(),0.5));
            gc.setLineWidth(0.5);
            gc.strokeOval(nodeX[d.id]-sz/2,nodeY[d.id]-sz/2,sz,sz);
        });

        // Stats overlay
        long inf=graph.dogs.values().stream().filter(d->d.infected).count();
        long vac=graph.dogs.values().stream().filter(d->d.vaccinated).count();
        gc.setFill(Color.web("#0d1117",0.78)); gc.fillRoundRect(10,10,185,88,10,10);
        gc.setFont(Font.font("Courier New",FontWeight.BOLD,11));
        ot(gc,28,"● Healthy:    "+(N_DOGS-inf-vac),HEALTHY);
        ot(gc,46,"● Infected:   "+inf,INFECTED);
        ot(gc,64,"● Vaccinated: "+vac,VACCINATED);
        gc.setFont(Font.font("Courier New",10)); gc.setFill(MUTED);
        gc.fillText("Infection rate: "+String.format("%.1f%%",inf*100.0/N_DOGS),20,82);
    }

    void ot(GraphicsContext gc, double y, String t, Color c) { gc.setFill(c); gc.fillText(t,20,y); }

    // ══════════════════════════════════════════════════════
    //  DRAW — Bar Chart
    // ══════════════════════════════════════════════════════
    void drawBarChart() {
        GraphicsContext gc=barCanvas.getGraphicsContext2D();
        double W=barCanvas.getWidth(),H=barCanvas.getHeight();
        double pL=44,pB=26,pT=14,pR=12,cW=W-pL-pR,cH=H-pB-pT;
        gc.setFill(PANEL); gc.fillRoundRect(0,0,W,H,8,8);

        Color[]  cols  ={HEALTHY,VACCINATED,ACCENT};
        String[] lbls  ={"Random","HiDeg","HiRisk"};
        double   barW  =cW/3-14;

        gc.setStroke(Color.web("#ffffff",0.05)); gc.setLineWidth(1);
        for (int g=0;g<=4;g++) {
            double y=pT+cH*(1-g/4.0);
            gc.strokeLine(pL,y,pL+cW,y);
            gc.setFont(Font.font("Courier New",9)); gc.setFill(MUTED);
            gc.fillText(String.format("%.0f",g/4.0*N_DOGS),4,y+4);
        }

        for (int s=0;s<3;s++) {
            double val=avgResults[s][1], barH=(val/N_DOGS)*cH;
            double x=pL+s*(cW/3)+7, y=pT+cH-barH; Color col=cols[s];
            gc.setFill(Color.color(col.getRed(),col.getGreen(),col.getBlue(),0.2));
            gc.fillRoundRect(x,pT,barW,cH,4,4);
            gc.setFill(col); gc.fillRoundRect(x,y,barW,barH,4,4);
            gc.setFont(Font.font("Courier New",FontWeight.BOLD,12)); gc.setFill(TEXT);
            gc.fillText(String.format("%.1f",val),x+barW/2-14,y-5);
            gc.setFont(Font.font("Courier New",10)); gc.setFill(col);
            gc.fillText(lbls[s],x+barW/2-14,H-7);
        }

        gc.save(); gc.translate(11,H/2+20); gc.rotate(-Math.PI/2);
        gc.setFont(Font.font("Courier New",9)); gc.setFill(MUTED);
        gc.fillText("Avg Infected",0,0); gc.restore();
    }

    // ══════════════════════════════════════════════════════
    //  DRAW — Line Chart
    // ══════════════════════════════════════════════════════
    void drawLineChart() {
        GraphicsContext gc=lineCanvas.getGraphicsContext2D();
        double W=lineCanvas.getWidth(),H=lineCanvas.getHeight();
        double pL=40,pB=24,pT=20,pR=12,cW=W-pL-pR,cH=H-pB-pT;
        gc.setFill(PANEL); gc.fillRoundRect(0,0,W,H,8,8);

        Color[]  cols =  {HEALTHY,VACCINATED,ACCENT};
        String[] lbls  = {"Random","HiDeg","HiRisk"};
        double   maxV  = 0;
        for (double[] row : perRunFinal) for (double v : row) maxV=Math.max(maxV,v);
        if (maxV==0) maxV=1;

        gc.setStroke(Color.web("#ffffff",0.05)); gc.setLineWidth(1);
        for (int g=0;g<=4;g++) {
            double y=pT+cH*(1-g/4.0); gc.strokeLine(pL,y,pL+cW,y);
            gc.setFont(Font.font("Courier New",9)); gc.setFill(MUTED);
            gc.fillText(String.format("%.0f",g/4.0*maxV),3,y+4);
        }

        for (int s=0;s<3;s++) {
            Color col=cols[s];
            double[] px=new double[RUNS], py=new double[RUNS];
            for (int r=0;r<RUNS;r++) {
                px[r]=pL+(r/(double)(RUNS-1))*cW;
                py[r]=pT+cH-(perRunFinal[s][r]/maxV)*cH;
            }
            gc.beginPath(); gc.moveTo(px[0],py[0]);
            for (int r=1;r<RUNS;r++) gc.lineTo(px[r],py[r]);
            gc.lineTo(px[RUNS-1],pT+cH); gc.lineTo(px[0],pT+cH); gc.closePath();
            gc.setFill(Color.color(col.getRed(),col.getGreen(),col.getBlue(),0.12)); gc.fill();
            gc.setStroke(col); gc.setLineWidth(2);
            gc.beginPath(); gc.moveTo(px[0],py[0]);
            for (int r=1;r<RUNS;r++) gc.lineTo(px[r],py[r]); gc.stroke();
            gc.setFill(col);
            for (int r=0;r<RUNS;r++) gc.fillOval(px[r]-3,py[r]-3,6,6);
        }

        gc.setFont(Font.font("Courier New",9)); gc.setFill(MUTED);
        for (int r=0;r<RUNS;r++) {
            double x=pL+(r/(double)(RUNS-1))*cW; gc.fillText(String.valueOf(r+1),x-3,H-5);
        }
        for (int s=0;s<3;s++) {
            gc.setFill(cols[s]); gc.fillRect(pL+s*110,3,10,8);
            gc.setFont(Font.font("Courier New",9)); gc.fillText(lbls[s],pL+s*110+14,11);
        }
    }

    // ══════════════════════════════════════════════════════
    //  ANIMATION
    // ══════════════════════════════════════════════════════
    void animateSpread() {
        if (animating) return;
        animating=true; runBtn.setDisable(true);
        statusLabel.setText("Animating: "+curStrat+"..."); progressBar.setProgress(0);

        graph.reset(); graph.infectRandom(INIT_INFECTED);
        List<Integer> initInf = new ArrayList<>();
        graph.dogs.values().stream().filter(d->d.infected).forEach(d->initInf.add(d.id));

        switch(curStrat) {
            case "Random":       graph.vaccinateRandom(VACCINES);      break;
            case "HighDegree":   graph.vaccinateHighDegree(VACCINES);  break;
            case "HighRiskArea": graph.vaccinateHighRiskArea(VACCINES); break;
        }
        List<List<Integer>> waves = graph.getWaves();

        graph.dogs.values().forEach(d->d.infected=false);
        initInf.forEach(id->graph.dogs.get(id).infected=true);
        switch(curStrat) {
            case "Random":       graph.vaccinateRandom(VACCINES);      break;
            case "HighDegree":   graph.vaccinateHighDegree(VACCINES);  break;
            case "HighRiskArea": graph.vaccinateHighRiskArea(VACCINES); break;
        }
        redrawNodes();

        Timeline tl=new Timeline();
        double delay=0; int total=Math.max(waves.size(),1);
        for (int w=0;w<waves.size();w++) {
            final List<Integer> wave=waves.get(w); final int wi=w;
            tl.getKeyFrames().add(new KeyFrame(Duration.seconds(delay), e -> {
                wave.forEach(id->graph.dogs.get(id).infected=true);
                redrawNodes(); progressBar.setProgress((double)(wi+1)/total);
            }));
            delay+=0.30;
        }
        tl.setOnFinished(e -> {
            animating=false; runBtn.setDisable(false);
            long inf=graph.dogs.values().stream().filter(d->d.infected).count();
            statusLabel.setText("Done! "+inf+" infected ("+String.format("%.1f%%",inf*100.0/N_DOGS)+")");
            progressBar.setProgress(1.0);
        });
        tl.play();
    }

    void redrawNodes() {
        GraphicsContext gc=graphCanvas.getGraphicsContext2D();
        double W=graphCanvas.getWidth(),H=graphCanvas.getHeight();
        gc.setFill(PANEL); gc.fillRoundRect(0,0,W,H,12,12);
        gc.setStroke(EDGE_COL); gc.setLineWidth(0.8);
        graph.dogs.values().forEach(d -> d.neighbors.forEach(nb -> {
            if (d.id<nb.id) gc.strokeLine(nodeX[d.id],nodeY[d.id],nodeX[nb.id],nodeY[nb.id]);
        }));
        graph.dogs.values().forEach(d -> {
            Color c=d.vaccinated?VACCINATED:d.infected?INFECTED:HEALTHY;
            double sz=4.5+Math.min(d.degree()*0.55,9);
            gc.setFill(c); gc.fillOval(nodeX[d.id]-sz/2,nodeY[d.id]-sz/2,sz,sz);
        });
    }

    String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255));
    }

    // ══════════════════════════════════════════════════════
    //  MAIN — Console simulation THEN JavaFX GUI
    // ══════════════════════════════════════════════════════
    public static void main(String[] args) {
        // Step 1: Run console simulation
        System.out.println("Building Scale-Free Graph (Barabasi-Albert)...");
        DogGraph   g   = DogGraph.buildScaleFreeGraph(N_DOGS, EDGES_PER_NODE);
        Experiment exp = new Experiment(g, RUNS, INIT_INFECTED, VACCINES);
        exp.compareStrategies();

        // Step 2: Launch JavaFX visual dashboard
        System.out.println("\nLaunching JavaFX Visual Dashboard...");
        launch(args);
    }
}
