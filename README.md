# Circuit Switching Simulator : Multi-Threaded Circuit Switching & TDM Network Simulator

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge)]()
[![Platform](https://img.shields.io/badge/Platform-Cross--Platform-orange?style=for-the-badge)]()

An enterprise-grade, highly interactive Java desktop application built to simulate, visualize, and mathematically analyze **Circuit-Switched Telecommunication Networks**. 

The system implements low-level **Time-Division Multiplexing (TDM)** slot allocation, multi-threaded connection signaling, atomic resource locking with automatic rollback, an automated **Discrete Event Poisson Traffic Engine**, and real-time statistical validation against theoretical **Erlang-B Call Blocking** models.

---

## Table of Contents
1. [Key Features](#-key-features)
2. [Deep-Dive Architecture & Modules](#-deep-dive-architecture--modules)
3. [Signaling Workflow & Protocol Flowchart](#-signaling-workflow--protocol-flowchart)
4. [Telecom Mathematical Theory (Erlang-B)](#-telecom-mathematical-theory-erlang-b)
5. [Class Structure & API Reference](#-class-structure--api-reference)
6. [Repository Layout](#-repository-layout)
7. [Getting Started & Build Instructions](#-getting-started--build-instructions)
8. [Comprehensive Testing & Verification](#-comprehensive-testing--verification)
9. [Future Roadmap](#-future-roadmap)
10. [License & Acknowledgments](#-license--acknowledgments)

---

## Key Features

### 1. Dynamic Java Swing Dashboard & Rendering Engine
* **Interactive Node Dragging:** Click and drag any node (Host or Switch) across the dark-mode workspace canvas to dynamically recalculate paths and visual layouts on the fly.
* **Animated Packet Flows:** Real-time visual rendering of active streaming data using gold packet animations moving along established paths.
* **Color-Coded Capacity Indicators:** Links dynamically change color based on usage state:
  * 🟢 **Forest Green:** Active link with free channel capacity.
  * 🔴 **Crimson Red:** Congested bottleneck link at 100% capacity.
  * 🔘 **Slate Grey:** Idle network link with 0% utilization.

### 2. Time-Division Multiplexing (TDM) Framing Logic
* Models TDM time slots on every directional link (`boolean[] timeSlots`).
* Ensures strict physical layer capacity enforcement—connections must reserve an identical frame slot index across multi-hop paths.

### 3. Thread-Safe Concurrency & Two-Phase Rollback
* Built using high-performance Java concurrency primitives (`ReentrantLock`, `ConcurrentHashMap`, and `CompletableFuture`).
* **Atomic Reservation:** Guarantees no race conditions occur when multiple concurrent caller threads target the same channel simultaneously.
* **Automatic Rollback Protocol:** If a call fails mid-path due to a bottleneck link, all previously reserved slots on earlier hops are automatically released in reverse order.

### 4. Discrete Event Poisson Traffic Engine
* Generates randomized stochastic traffic simulating real-world PSTN/optical networks:
  * **Inter-arrival Times ($\Delta t$):** Modeled using a Poisson process with rate parameter $\lambda$.
  * **Call Holding Times ($H$):** Modeled using an Exponential distribution with mean duration $\mu$.
* Configurable live sliders to dynamically adjust traffic intensity during runtime.

### 5. Live Analytics & Erlang-B Mathematical Verification
* Includes a custom, real-time line graph plotting **Call Blocking Probability ($P_{block}$)** against sample time.
* Calculates theoretical $P_{block}$ dynamically using the recursive **Erlang-B formula** and overlays empirical performance against theoretical telecom expectations.

---

## Deep-Dive Architecture & Modules

```
+--------------------------------------------------------------------------+
|                     SIMULATION & VISUALIZATION LAYER                     |
|  - Custom Graphics2D Workspace Canvas    - Live Line-Chart Rendering     |
|  - Mouse Listener Node Drag Handler      - Interactive Control Panel     |
+--------------------------------------------------------------------------+
                                    │
                                    ▼
+--------------------------------------------------------------------------+
|                        CIRCUIT MANAGEMENT ENGINE                         |
|  - Breadth-First Search (BFS) Pathfinding - Atomic Reservation Protocol  |
|  - End-to-End Resource Rollback Handler  - CompletableFuture Scheduler   |
+--------------------------------------------------------------------------+
                                    │
                                    ▼
+--------------------------------------------------------------------------+
|                     CONCURRENCY & TDM TRANSPORT LAYER                    |
|  - ReentrantLock Fine-Grained Protection - TDM Time-Slot Grid Arrays     |
+--------------------------------------------------------------------------+
                                    │
                                    ▼
+--------------------------------------------------------------------------+
|                    TELECOM MATHEMATICS & DES MODULE                      |
|  - Poisson Arrival Engine (λ)          - Exponential Hold Generator (μ)  |
|  - Recursive Erlang-B Mathematical Evaluator                             |
+--------------------------------------------------------------------------+
```

---

## Telecom Mathematical Theory (Erlang-B)

The system evaluates connection loss probability $P_{block} = B(E, m)$ using the classical **Erlang-B formula**, where $E$ represents the offered load in Erlangs and $m$ represents trunk capacity:

$$E = \lambda \cdot h$$

$$B(E, m) = \frac{\frac{E^m}{m!}}{\sum_{k=0}^{m} \frac{E^k}{k!}}$$

### Numerical Stability Optimization Formula
To prevent arithmetic overflow caused by high factorials ($m!$) during runtime execution, the simulator computes the inverse Erlang-B ratio iteratively:

$$\frac{1}{B(E, k)} = 1 + \frac{k}{E} \cdot \frac{1}{B(E, k-1)} \quad \text{for } k = 1, 2, \dots, m$$

```java
public static double calculateErlangB(double trafficErlangs, int channels) {
    if (channels <= 0) return 1.0;
    double invB = 1.0;
    for (int i = 1; i <= channels; i++) {
        invB = 1.0 + invB * (i / trafficErlangs);
    }
    return 1.0 / invB;
}
```

---
## Class Structure & API Reference

| Class Name | Primary Responsibility | Key Methods / Fields |
| :--- | :--- | :--- |
| `CircuitSwitchingProject` | Main window container, layout manager, and simulation supervisor. | `main()`, `createControlDashboard()`, `buildInitialTopology()` |
| `TDMThreadSafeLink` | Models physical connection link with TDM channels and concurrency locks. | `reserveSlot()`, `releaseSlot()`, `ReentrantLock` |
| `NodeModel` | Represents structural coordinates and entity type (Host vs Switch). | `contains(Point p)`, `x`, `y`, `isHost` |
| `ActiveCircuit` | Data class storing circuit metadata and reserved slot indices. | `id`, `path`, `linkSlotAssignments` |
| `ErlangBAnalyzer` | Mathematical evaluation engine calculating theoretical blocking probabilities. | `calculateErlangB(double E, int m)` |
| `NetworkCanvas` | Custom `JPanel` rendering nodes, link states, animations, and mouse-drag events. | `paintComponent()`, `MouseAdapter` |
| `LiveChartPanel` | Real-time graph drawing engine plotting $P_{block}$ over discrete sample steps. | `paintComponent()`, `blockingRateHistory` |

---
## Getting Started & Build Instructions

### Prerequisites
* **Java Development Kit (JDK 17 or higher)** installed and added to `PATH`.
* Terminal, Command Prompt, or any Java IDE (VS Code, IntelliJ IDEA, Eclipse).

### 1. Clone Repository
```
git clone [https://github.com/Sindhuja-M-309/Circuit-Switching-Simulator.git](https://github.com/YOUR_USERNAME/Circuit-Switching-Simulator.git)
cd Circuit-Switching-Simulator
```
### 2. Compile Codebase
```
# Direct compilation of single-file suite
javac src/CircuitSwitchingProject.java
```
### 3. Run Application
```# Launch the application UI
java -cp src CircuitSwitchingProject
```
---

## Comprehensive Testing & Verification

The system includes pre-configured test scenarios to validate protocol handling :

| Test Case | Test Scenario | Input Steps | Expected System Behavior | Result |
| :---: | :--- | :--- | :--- | :---: |
| **TC-01** | **Single Connection Establishment** | Select `HostA` $\to$ `HostB`, Click *Establish Connection*. | Circuit created (`CIRCUIT-00
