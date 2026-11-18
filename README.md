# 📡 CircuitSwitchSim: Multi-Threaded Circuit Switching & TDM Network Simulator

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge)]()
[![Platform](https://img.shields.io/badge/Platform-Cross--Platform-orange?style=for-the-badge)]()

An enterprise-grade, highly interactive Java desktop application built to simulate, visualize, and mathematically analyze **Circuit-Switched Telecommunication Networks**. 

The system implements low-level **Time-Division Multiplexing (TDM)** slot allocation, multi-threaded connection signaling, atomic resource locking with automatic rollback, an automated **Discrete Event Poisson Traffic Engine**, and real-time statistical validation against theoretical **Erlang-B Call Blocking** models.

---

## 📑 Table of Contents
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

## 🌟 Key Features

### 🎨 1. Dynamic Java Swing Dashboard & Rendering Engine
* **Interactive Node Dragging:** Click and drag any node (Host or Switch) across the dark-mode workspace canvas to dynamically recalculate paths and visual layouts on the fly.
* **Animated Packet Flows:** Real-time visual rendering of active streaming data using gold packet animations moving along established paths.
* **Color-Coded Capacity Indicators:** Links dynamically change color based on usage state:
  * 🟢 **Forest Green:** Active link with free channel capacity.
  * 🔴 **Crimson Red:** Congested bottleneck link at 100% capacity.
  * 🔘 **Slate Grey:** Idle network link with 0% utilization.

### ⏱️ 2. Time-Division Multiplexing (TDM) Framing Logic
* Models TDM time slots on every directional link (`boolean[] timeSlots`).
* Ensures strict physical layer capacity enforcement—connections must reserve an identical frame slot index across multi-hop paths.

### 🔒 3. Thread-Safe Concurrency & Two-Phase Rollback
* Built using high-performance Java concurrency primitives (`ReentrantLock`, `ConcurrentHashMap`, and `CompletableFuture`).
* **Atomic Reservation:** Guarantees no race conditions occur when multiple concurrent caller threads target the same channel simultaneously.
* **Automatic Rollback Protocol:** If a call fails mid-path due to a bottleneck link, all previously reserved slots on earlier hops are automatically released in reverse order.

### 📊 4. Discrete Event Poisson Traffic Engine
* Generates randomized stochastic traffic simulating real-world PSTN/optical networks:
  * **Inter-arrival Times ($\Delta t$):** Modeled using a Poisson process with rate parameter $\lambda$.
  * **Call Holding Times ($H$):** Modeled using an Exponential distribution with mean duration $\mu$.
* Configurable live sliders to dynamically adjust traffic intensity during runtime.

### 📈 5. Live Analytics & Erlang-B Mathematical Verification
* Includes a custom, real-time line graph plotting **Call Blocking Probability ($P_{block}$)** against sample time.
* Calculates theoretical $P_{block}$ dynamically using the recursive **Erlang-B formula** and overlays empirical performance against theoretical telecom expectations.

---

## 🏗️ Deep-Dive Architecture & Modules

```text
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
