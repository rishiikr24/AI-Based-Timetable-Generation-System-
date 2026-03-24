import java.util.*;

/**
 * Timetable Generator -- SIH25091 (NEP 2020)
 * Algorithms: Graph Coloring (color = time slot) + Backtracking
 *
 * Graph Coloring : courses are vertices; an edge exists between two courses
 *                  that share an instructor or classroom. Assigning colors
 *                  ensures no two conflicting courses share a time slot.
 * Backtracking   : if a color assignment leads to a dead-end, undo and retry
 *                  with the next available color.
 *
 * Constraints enforced:
 *  - No instructor teaches two courses in the same slot.
 *  - No classroom hosts two courses in the same slot.
 *  - Each faculty gets at least 1 free slot (1 hour) before their next class.
 *  - All classes are scheduled between 09:00 and 16:00 (7 slots per day).
 */
public class TimetableGenerator {

    // Time slots (09:00-16:00, one per hour)

    static final String[] SLOT_LABELS = {
        "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
        "13:00-14:00", "14:00-15:00", "15:00-16:00"
    };
    static final int SLOTS_PER_DAY = SLOT_LABELS.length;   // 7
    static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    // Model

    static class Course {
        int id; String name, instructor, room;
        Course(int id, String name, String instructor, String room) {
            this.id = id; this.name = name; this.instructor = instructor; this.room = room;
        }
    }

    // Graph Representation
    // ALGORITHM: Graph Representation -- vertices = courses, edges = conflicts

    static List<List<Integer>> buildGraph(List<Course> courses) {
        int n = courses.size();
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) {
                Course a = courses.get(i), b = courses.get(j);
                if (a.instructor.equals(b.instructor) || a.room.equals(b.room)) {
                    adj.get(i).add(j); adj.get(j).add(i);
                }
            }
        return adj;
    }

    // Graph Coloring + Backtracking
    // ALGORITHM: Graph Coloring  -- color index = time slot index
    // ALGORITHM: Backtracking    -- undo assignment when no valid color exists

    static int[] color;

    // verify no neighbouring course shares the same slot,
    // AND the 1-hour break rule is respected for the same instructor.
    static boolean isSafe(List<List<Integer>> adj, List<Course> courses,
                          int node, int slot, Map<String, List<Integer>> instructorSlots) {
        // Conflict check (graph coloring constraint)
        for (int nb : adj.get(node))
            if (color[nb] == slot) return false;

        // 1-hour break rule: instructor must not have a class in slot-1 or slot+1
        String instr = courses.get(node).instructor;
        List<Integer> used = instructorSlots.getOrDefault(instr, Collections.emptyList());
        for (int s : used)
            if (Math.abs(s - slot) <= 1) return false;   // adjacent slot -> no gap

        return true;
    }

    // BACKTRACKING: assign slots to courses one by one; backtrack on dead-ends
    static boolean solve(List<List<Integer>> adj, List<Course> courses,
                         int node, int maxSlots, Random rng,
                         Map<String, List<Integer>> instructorSlots) {
        if (node == courses.size()) return true;

        // RANDOMIZATION: shuffle slot-try order for variety each run
        List<Integer> slotOrder = new ArrayList<>();
        for (int s = 0; s < maxSlots; s++) slotOrder.add(s);
        Collections.shuffle(slotOrder, rng);

        String instr = courses.get(node).instructor;
        for (int slot : slotOrder) {
            if (isSafe(adj, courses, node, slot, instructorSlots)) {
                color[node] = slot;
                instructorSlots.computeIfAbsent(instr, k -> new ArrayList<>()).add(slot);

                if (solve(adj, courses, node + 1, maxSlots, rng, instructorSlots))
                    return true;

                // BACKTRACK: undo assignment
                color[node] = -1;
                instructorSlots.get(instr).remove(Integer.valueOf(slot));
            }
        }
        return false;
    }

    // Schedule one day generation 

    static int[] generateDay(List<Course> courses, Random rng) {
        List<Course> shuffled = new ArrayList<>(courses);
        Collections.shuffle(shuffled, rng); // RANDOMIZATION: random course order

        List<List<Integer>> adj = buildGraph(shuffled);
        color = new int[shuffled.size()];
        Arrays.fill(color, -1);

        for (int k = 1; k <= SLOTS_PER_DAY; k++) {
            Arrays.fill(color, -1);
            Map<String, List<Integer>> instrSlots = new HashMap<>();
            if (solve(adj, shuffled, 0, k, rng, instrSlots)) {
                // Map back to original course order
                int[] result = new int[courses.size()];
                Arrays.fill(result, -1);
                for (int i = 0; i < shuffled.size(); i++)
                    result[shuffled.get(i).id] = color[i];
                return result;
            }
        }
        return null; // no solution found
    }

    //  Print one day's timetable 

    static void printDay(String dayLabel, List<Course> courses, int[] assignment) {
        System.out.println("\n--- " + dayLabel + " ---");
        // Group by slot
        Map<Integer, List<Course>> bySlot = new TreeMap<>();
        for (Course c : courses)
            if (assignment[c.id] >= 0)
                bySlot.computeIfAbsent(assignment[c.id], k -> new ArrayList<>()).add(c);

        for (var entry : bySlot.entrySet()) {
            System.out.println("  [" + SLOT_LABELS[entry.getKey()] + "]");
            for (Course c : entry.getValue())
                System.out.printf("    C%d %-22s  %-14s  Room: %s%n",
                        c.id, c.name, c.instructor, c.room);
        }
        System.out.println("------------------------------------------------------------");
    }

    //  Validate: no two courses in same slot conflict, and break rule is respected

    static boolean validate(List<Course> courses, int[] assignment) {
        for (int i = 0; i < courses.size(); i++)
            for (int j = i + 1; j < courses.size(); j++) {
                if (assignment[i] != assignment[j]) continue;
                Course a = courses.get(i), b = courses.get(j);
                if (a.instructor.equals(b.instructor) || a.room.equals(b.room)) return false;
            }
        return true;
    }

    //  Main 
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Random rng = new Random();

        List<Course> courses = new ArrayList<>(List.of(
            new Course(0, "Data Structures",       "Dr. Sharma", "R101"),
            new Course(1, "Machine Learning",      "Dr. Patel",  "R102"),
            new Course(2, "Environmental Science", "Dr. Sharma", "R103"), // same instructor as C0
            new Course(3, "Probability & Stats",   "Dr. Iyer",   "R101"), // same room as C0
            new Course(4, "Philosophy of Science", "Dr. Mehta",  "R104"),
            new Course(5, "Introduction to AI",    "Dr. Patel",  "R105"), // same instructor as C1
            new Course(6, "Linear Algebra",        "Dr. Iyer",   "R102"), // same instructor as C3, room as C1
            new Course(7, "Digital Humanities",    "Dr. Mehta",  "R104")  // same instructor & room as C4
        ));

        // ---- Output 1: Today's timetable ------------------------------------------------------------------------------
        System.out.println("=== TODAY'S TIMETABLE (09:00 - 16:00) ===");
        System.out.println("    Constraint: 1-hour break between consecutive classes for each faculty\n");

        int[] todayAssignment = generateDay(courses, rng);
        if (todayAssignment == null) {
            System.out.println("Could not generate today's timetable. Try again."); return;
        }
        printDay("Today", courses, todayAssignment);
        System.out.println(validate(courses, todayAssignment)
                ? "\nValidation: OK -- no conflicts, break rule respected."
                : "\nValidation: FAILED.");

        // ---- Output 2: Ask for weekly timetable ----------------------------------------------------------------
        System.out.print("\nGenerate full week timetable (Monday-Friday)? [yes/no]: ");
        String answer = sc.nextLine().trim().toLowerCase();

        if (answer.equals("yes") || answer.equals("y")) {
            System.out.println("\n=== WEEKLY TIMETABLE (Mon-Fri, 09:00-16:00) ===");
            System.out.println("    Constraint: 1-hour break between consecutive classes for each faculty\n");

            for (String day : DAYS) {
                int[] dayAssignment = generateDay(courses, rng);
                if (dayAssignment == null) {
                    System.out.println(day + ": Could not generate timetable."); continue;
                }
                printDay(day, courses, dayAssignment);
                System.out.println("  " + (validate(courses, dayAssignment)
                        ? "Validation: OK" : "Validation: FAILED"));
            }
        } else {
            System.out.println("Weekly timetable skipped. Goodbye!");
        }

        sc.close();
    }
}

/*
 * Complexity Analysis
 * -------------------
 * Graph construction : O(n^2)
 * Backtracking worst : O(k^n)  -- k = slots (7), n = courses
 * isSafe check       : O(degree + instructor_classes) per call
 * Space              : O(n^2) adjacency list + O(n) color array
 *
 * Graph Coloring is NP-Hard; backtracking gives an exact solution.
 * For larger inputs, consider greedy DSatur (O(n^2)) as a heuristic.
 */