#  Canine Vaccine Strategy Simulator

> A Java-based epidemic simulation tool that models disease spread in urban dog populations using graph algorithms and compares three vaccination strategies to identify the most effective approach for disease control.

---

##  Live Demo
 **[Click here to view Interactive Dashboard](https://erlekar13.github.io/Canine-Vaccine-Strategy-Simulator/SimulatorDashboard.html)**

---

##  About the Project

Urban dog populations form social contact networks. When a disease breaks out, the spread depends heavily on **which dogs get vaccinated** and **how the contact network is structured**.

This simulator models that problem as a **graph algorithm challenge** and answers:
> **Which vaccination strategy saves the most lives?**

The project runs 10 independent simulations per strategy and compares results to find the optimal approach — directly applicable to real-world public health decision making.

---

## Features

- **Epidemic Simulation** — Probabilistic SI model (40% infection probability per contact)
- **Realistic Graph Model** — Scale-Free network using Barabási–Albert preferential attachment
- **3 Vaccination Strategies** — Random, High-Degree (Hub), High-Risk Area (Ring)
- **JavaFX Visual Dashboard** — Live animated graph + Bar chart + Line chart
- **Interactive HTML Dashboard** — Works in any browser, no setup needed
- **CSV Export** — Auto-saves results to `simulation_results.csv`
- **15 JUnit Tests** — Full test coverage of all strategies and edge cases
- **CLI Support** — Custom parameters via command line arguments

---

## Core Concepts & Data Structures

| Concept | Application |
|---|---|
| **Graph (Adjacency List)** | Dogs = nodes, contacts = edges |
| **BFS (Breadth-First Search)** | Wave-by-wave infection spread — O(V+E) |
| **HashMap** | O(1) dog lookup by ID |
| **Queue (LinkedList)** | FIFO processing for BFS spread |
| **HashSet** | Avoid duplicate vaccinations in ring strategy |
| **Scale-Free Graph** | Barabási–Albert preferential attachment model |
| **Greedy Algorithm** | High-Degree strategy targets hub nodes first |

---

## 💉 Vaccination Strategies

### 1.  Random Vaccination
Vaccinate randomly chosen dogs. Cheapest to implement. Used as **baseline**.

### 2.  High-Degree (Hub Targeting)
Vaccinate the most socially connected dogs first. Breaks the most transmission paths.
Most effective in scale-free networks — based on **herd immunity** principles.

### 3.  High-Risk Area (Ring Vaccination)
Vaccinate neighbors of initially infected dogs. Targets the immediate spread zone.
Effective for **early-stage outbreak containment**.

---

##  Sample Output

```
=================================================================
   CANINE VACCINE STRATEGY SIMULATOR v3.0
=================================================================
  Dogs: 100 | Infected: 5 | Vaccines: 30 | Runs: 10
  Inf Prob: 40% | Avg Degree: 5.88 | Max Degree: 28
=================================================================

Strategy       AvgEverInfected    AvgFinalInfected   AvgVaccinated
-----------------------------------------------------------------
Random         51.30              51.30              30.00
HighDegree     13.70              13.70              30.00
★ HighRiskArea  8.60               8.60               30.00
-----------------------------------------------------------------
  Best Strategy  : HighRiskArea
  Improvement    : 83.2% over Random
=================================================================
```

---

##  Project Structure

```
Canine-Vaccine-Strategy-Simulator/
├── DogVaccinationApp.java         ← Main file (Simulation + JavaFX GUI)
│   ├── Dog                        ← Graph node (id, infected, vaccinated, neighbors)
│   ├── DogGraph                   ← Graph + Scale-Free builder + 3 strategies + BFS
│   ├── SimulationResult           ← Per-run data holder
│   └── Experiment                 ← Multi-run orchestrator + CSV export
├── DogVaccinationAppTest.java     ← 15 JUnit 5 unit tests
├── SimulatorDashboard.html        ← Interactive browser visualization
└── simulation_results.csv         ← Auto-generated after run
```

---

##  Tech Stack

| Tool | Purpose |
|---|---|
| Java 11+ | Core simulation engine |
| JavaFX | Visual desktop dashboard |
| JUnit 5 | Unit testing |
| HTML + CSS + JS | Browser-based interactive dashboard |
| GitHub Pages | Live demo hosting |

---

##  How to Run

### Option 1 — Console Only (No JavaFX needed)
```bash
javac DogVaccinationApp.java
java DogVaccinationApp
```

### Option 2 — Full JavaFX Dashboard (Eclipse)
```
1. Download JavaFX SDK from https://gluonhq.com/products/javafx/
2. Right-click project → Properties → Java Build Path → Add External JARs
3. Add all jars from javafx-sdk/lib/
4. Right-click file → Run As → Run Configurations → VM Arguments:
   --module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls
5. Click Run
```

### Option 3 — Custom Parameters
```bash
java DogVaccinationApp <numDogs> <initialInfected> <vaccines> <runs>

# Example: 200 dogs, 10 infected, 50 vaccines, 20 runs
java DogVaccinationApp 200 10 50 20
```

### Option 4 — HTML Dashboard (Easiest — Zero Setup!)
```
Simply open SimulatorDashboard.html in Chrome/Firefox
OR visit the Live Demo link at the top ↑
```

---

##  Unit Tests (15 Tests)

| Test | What it Checks |
|---|---|
| `testRandomGraphNodeCount` | Graph has correct number of nodes |
| `testScaleFreeGraphHasHub` | Hub nodes exist (max degree >> avg) |
| `testNoSelfLoops` | No dog is its own neighbor |
| `testUndirectedEdges` | All edges are bidirectional |
| `testReset` | Reset clears all flags correctly |
| `testHighDegreeVaccinatesMostConnected` | Hub dog vaccinated first |
| `testHighRiskAreaTargetsNeighbors` | Neighbors of infected are targeted |
| `testVaccinatedDogsNotInfected` | Vaccine blocks spread |
| `testRunsAreIndependent` | Each run gives statistically varied results |
| `testHighDegreeBeatsRandomOnScaleFree` | HighDegree wins on scale-free graph |

---

## Algorithm Complexity

| Operation | Time Complexity | Space Complexity |
|---|---|---|
| Build Scale-Free Graph | O(N² × E) | O(N + E) |
| BFS Infection Spread | O(V + E) | O(V) |
| HighDegree Strategy (sort) | O(V log V) | O(V) |
| Random Strategy | O(V) | O(V) |
| HighRiskArea Strategy | O(V + E) | O(V) |

---

## 📈 Key Findings

- **HighRiskArea** reduces infections by **~83%** vs Random when outbreak location is known
- **HighDegree** reduces infections by **~70%** and works **without knowing** initial infected dogs
- Vaccinating the **top 5% most connected dogs** prevents majority of infections
- Scale-free networks have hub dogs responsible for most transmission — targeting them is critical

---

## 🔭 Future Enhancements

- [ ] SIR Model — add Recovery state with recovery probability
- [ ] Multi-wave seasonal outbreak simulation
- [ ] Partial vaccine effectiveness (< 100% immunity)
- [ ] Community-based graph model
- [ ] Export visual graph as image

---

## 📚 References

- Barabási, A.L. & Albert, R. (1999). *Emergence of Scaling in Random Networks*
- Kermack & McKendrick (1927). *SIR Epidemic Model*
- Newman, M.E.J. (2003). *The Structure and Function of Complex Networks*

---

## 👤 Author

**Erlekar** — [GitHub Profile](https://github.com/erlekar13)

⭐ If you found this project useful, please consider starring the repository!
