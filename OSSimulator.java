import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

// Main Application Class
public class OSSimulator {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame();
        });
    }
}

// Main Frame
class MainFrame extends JFrame {
    private Kernel kernel;
    
    public MainFrame() {
        kernel = new Kernel();
        setTitle("Tabinda OS - Operating System Simulator");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));
        
        // Header
        JPanel header = new JPanel();
        header.setBackground(new Color(45, 45, 45));
        JLabel title = new JLabel("Tabinda Simulator");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title);
        
        // Main Control Panel
        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(240, 240, 240));
        
        // Create main buttons
        JButton processBtn = createMainButton("Process Management");
        JButton memoryBtn = createMainButton("Memory Management");
        JButton ioBtn = createMainButton("I/O Management");
        JButton otherBtn = createMainButton("Other Operations");
        
        processBtn.addActionListener(e -> new ProcessManagementWindow(kernel));
        memoryBtn.addActionListener(e -> new MemoryManagementWindow(kernel));
        ioBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "I/O Management Module"));
        otherBtn.addActionListener(e -> new ConfigurationWindow(kernel));
        
        mainPanel.add(processBtn);
        mainPanel.add(memoryBtn);
        mainPanel.add(ioBtn);
        mainPanel.add(otherBtn);
        
        add(header, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private JButton createMainButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setBackground(new Color(70, 130, 180));
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(150, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(100, 149, 237));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(70, 130, 180));
            }
        });
        
        return btn;
    }
}

// Process Control Block
class PCB {
    private static int idCounter = 1;
    private int processId;
    private ProcessState state;
    private String owner;
    private int priority;
    private int memoryRequirement;
    private int burstTime;
    private int arrivalTime;
    private int remainingTime;
    private List<Integer> pageNumbers;
    
    public PCB(String owner, int priority, int memoryReq, int burstTime, int arrivalTime) {
        this.processId = idCounter++;
        this.state = ProcessState.NEW;
        this.owner = owner;
        this.priority = priority;
        this.memoryRequirement = memoryReq;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.pageNumbers = new ArrayList<>();
        this.arrivalTime = arrivalTime;
    }
    
    // Getters and Setters
    public int getProcessId() { return processId; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getBurstTime() { return burstTime; }
    public int getRemainingTime() { return remainingTime; }
    public void setRemainingTime(int time) { this.remainingTime = time; }
    public int getArrivalTime() { return arrivalTime; }
    public String getOwner() { return owner; }
    public int getMemoryRequirement() { return memoryRequirement; }
    public List<Integer> getPageNumbers() { return pageNumbers; }
    public void addPage(int pageNum) { pageNumbers.add(pageNum); }
    
    @Override
    public String toString() {
        return String.format("P%d [%s] Pri:%d Burst:%d AT:%d", processId, state, priority, burstTime, arrivalTime);
    }
}

// Process States
enum ProcessState {
    NEW, READY, RUNNING, BLOCKED, SUSPENDED, TERMINATED
}

// Partition Class for Fixed Partitioning
class Partition {
    private int id;
    private int startAddress;
    private int size;
    private boolean allocated;
    private int processId;
    
    public Partition(int id, int startAddress, int size) {
        this.id = id;
        this.startAddress = startAddress;
        this.size = size;
        this.allocated = false;
        this.processId = -1;
    }
    
    public int getId() { return id; }
    public int getStartAddress() { return startAddress; }
    public int getSize() { return size; }
    public boolean isAllocated() { return allocated; }
    public int getProcessId() { return processId; }
    
    public void allocate(int processId) {
        this.allocated = true;
        this.processId = processId;
    }
    
    public void deallocate() {
        this.allocated = false;
        this.processId = -1;
    }
    
    public int getEndAddress() {
        return startAddress + size;
    }
}

// Kernel - Core OS
class Kernel {
    private List<PCB> allProcesses;
    private Queue<PCB> readyQueue;
    private Queue<PCB> blockedQueue;
    private List<PCB> suspendedProcesses;
    private PCB runningProcess;
    private int pageSize = 4096; // Default 4KB
    private Map<Integer, Page> pageTable;
    private Semaphore mutex;
    
    // Fixed Partitioning
    private int totalMemory = 1024; // Total memory in KB
    private List<Partition> partitions;
    
    public Kernel() {
        allProcesses = new ArrayList<>();
        readyQueue = new LinkedList<>();
        blockedQueue = new LinkedList<>();
        suspendedProcesses = new ArrayList<>();
        pageTable = new HashMap<>();
        mutex = new Semaphore(1);
        partitions = new ArrayList<>();
        initializeFixedPartitions();
    }
    
    private void initializeFixedPartitions() {
        // Create fixed partitions of different sizes
        partitions.add(new Partition(1, 0, 100));
        partitions.add(new Partition(2, 100, 200));
        partitions.add(new Partition(3, 300, 150));
        partitions.add(new Partition(4, 450, 250));
        partitions.add(new Partition(5, 700, 324));
    }
    
    public List<Partition> getPartitions() {
        return partitions;
    }
    
    public String allocateFixedPartition(PCB process) {
        // First Fit algorithm
        for (Partition p : partitions) {
            if (!p.isAllocated() && p.getSize() >= process.getMemoryRequirement()) {
                p.allocate(process.getProcessId());
                return String.format("Process P%d allocated to Partition %d\nPartition Size: %d KB\nProcess Size: %d KB\nInternal Fragmentation: %d KB",
                        process.getProcessId(), p.getId(), p.getSize(), 
                        process.getMemoryRequirement(), p.getSize() - process.getMemoryRequirement());
            }
        }
        return "No suitable partition found! Process P" + process.getProcessId() + " cannot be allocated.";
    }
    
    public String deallocateFixedPartition(int partitionId) {
        for (Partition p : partitions) {
            if (p.getId() == partitionId && p.isAllocated()) {
                int processId = p.getProcessId();
                p.deallocate();
                return String.format("Partition %d deallocated (was holding Process P%d)", partitionId, processId);
            }
        }
        return "Partition " + partitionId + " is not allocated or doesn't exist!";
    }
    
    public void resetPartitions() {
        for (Partition p : partitions) {
            p.deallocate();
        }
    }
    
    public PCB createProcess(String owner, int priority, int memoryReq, int burstTime, int arrivalTime) {
        PCB pcb = new PCB(owner, priority, memoryReq, burstTime, arrivalTime);
        allProcesses.add(pcb);
        allocateMemory(pcb);
        pcb.setState(ProcessState.READY);
        readyQueue.add(pcb);
        return pcb;
    }
    
    public void destroyProcess(PCB pcb) {
        if (pcb == runningProcess) runningProcess = null;
        allProcesses.remove(pcb);
        readyQueue.remove(pcb);
        blockedQueue.remove(pcb);
        suspendedProcesses.remove(pcb);
        deallocateMemory(pcb);
    }
    
    public void suspendProcess(PCB pcb) {
        pcb.setState(ProcessState.SUSPENDED);
        readyQueue.remove(pcb);
        if (runningProcess == pcb) runningProcess = null;
        suspendedProcesses.add(pcb);
    }
    
    public void resumeProcess(PCB pcb) {
        if (pcb.getState() == ProcessState.SUSPENDED) {
            suspendedProcesses.remove(pcb);
            pcb.setState(ProcessState.READY);
            readyQueue.add(pcb);
        }
    }
    
    public void blockProcess(PCB pcb) {
        pcb.setState(ProcessState.BLOCKED);
        readyQueue.remove(pcb);
        if (runningProcess == pcb) runningProcess = null;
        blockedQueue.add(pcb);
    }
    
    public void wakeupProcess(PCB pcb) {
        if (pcb.getState() == ProcessState.BLOCKED) {
            blockedQueue.remove(pcb);
            pcb.setState(ProcessState.READY);
            readyQueue.add(pcb);
        }
    }
    
    public void dispatchProcess(PCB pcb) {
        if (runningProcess != null) {
            runningProcess.setState(ProcessState.READY);
            readyQueue.add(runningProcess);
        }
        runningProcess = pcb;
        pcb.setState(ProcessState.RUNNING);
    }
    
    public void changePriority(PCB pcb, int newPriority) {
        pcb.setPriority(newPriority);
    }
    
    // Scheduling Algorithms
    public void scheduleFCFS() {
        if (!readyQueue.isEmpty() && runningProcess == null) {
            PCB next = readyQueue.poll();
            dispatchProcess(next);
        }
    }
    
    public void scheduleSJF() {
        if (runningProcess == null && !readyQueue.isEmpty()) {
            PCB shortest = null;
            for (PCB p : readyQueue) {
                if (shortest == null || p.getBurstTime() < shortest.getBurstTime()) {
                    shortest = p;
                }
            }
            if (shortest != null) {
                readyQueue.remove(shortest);
                dispatchProcess(shortest);
            }
        }
    }
    
    public void scheduleRoundRobin() {
        if (runningProcess != null) {
            runningProcess.setRemainingTime(runningProcess.getRemainingTime() - 1);
            if (runningProcess.getRemainingTime() <= 0) {
                runningProcess.setState(ProcessState.TERMINATED);
                runningProcess = null;
            } else if (Math.random() > 0.7) { // Simulate time quantum
                runningProcess.setState(ProcessState.READY);
                readyQueue.add(runningProcess);
                runningProcess = null;
            }
        }
        if (runningProcess == null && !readyQueue.isEmpty()) {
            dispatchProcess(readyQueue.poll());
        }
    }
    
    // Memory Management
    private void allocateMemory(PCB pcb) {
        int pagesNeeded = (int) Math.ceil((double) pcb.getMemoryRequirement() / pageSize);
        for (int i = 0; i < pagesNeeded; i++) {
            int pageNum = pageTable.size();
            pageTable.put(pageNum, new Page(pageNum, pcb.getProcessId()));
            pcb.addPage(pageNum);
        }
    }
    
    private void deallocateMemory(PCB pcb) {
        for (int pageNum : pcb.getPageNumbers()) {
            pageTable.remove(pageNum);
        }
    }
    
    public String applyLRU(int maxPages) {
        if (pageTable.size() <= maxPages) {
            return "No pages need to be replaced. Current pages: " + pageTable.size() + ", Max allowed: " + maxPages;
        }
        
        // Find the least recently used page
        Page lruPage = null;
        int lruPageNum = -1;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<Integer, Page> entry : pageTable.entrySet()) {
            if (entry.getValue().getLastAccessed() < oldestTime) {
                oldestTime = entry.getValue().getLastAccessed();
                lruPage = entry.getValue();
                lruPageNum = entry.getKey();
            }
        }
        
        if (lruPage != null) {
            pageTable.remove(lruPageNum);
            return String.format("Removed Page #%d (Process P%d) - Least Recently Used\nRemaining pages: %d", 
                    lruPageNum, lruPage.getProcessId(), pageTable.size());
        }
        
        return "No pages to remove";
    }
    
    public void simulatePageAccess() {
        // Simulate random page accesses to create different access times
        if (!pageTable.isEmpty()) {
            List<Integer> pageNums = new ArrayList<>(pageTable.keySet());
            Random rand = new Random();
            for (int i = 0; i < 3; i++) {
                int randomPage = pageNums.get(rand.nextInt(pageNums.size()));
                try {
                    Thread.sleep(10); // Small delay to create time difference
                } catch (InterruptedException e) {}
                pageTable.get(randomPage).access();
            }
        }
    }
    
    // Getters
    public List<PCB> getAllProcesses() { return allProcesses; }
    public Queue<PCB> getReadyQueue() { return readyQueue; }
    public Queue<PCB> getBlockedQueue() { return blockedQueue; }
    public PCB getRunningProcess() { return runningProcess; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int size) { this.pageSize = size; }
    public Map<Integer, Page> getPageTable() { return pageTable; }
    public Semaphore getMutex() { return mutex; }
}

// Page Class
class Page {
    private int pageNumber;
    private int processId;
    private long lastAccessed;
    private int accessCount;
    
    public Page(int pageNumber, int processId) {
        this.pageNumber = pageNumber;
        this.processId = processId;
        this.lastAccessed = System.currentTimeMillis();
        this.accessCount = 0;
    }
    
    public int getPageNumber() { return pageNumber; }
    public int getProcessId() { return processId; }
    public long getLastAccessed() { return lastAccessed; }
    public int getAccessCount() { return accessCount; }
    public void access() { 
        lastAccessed = System.currentTimeMillis(); 
        accessCount++;
    }
}

// Process Management Window
class ProcessManagementWindow extends JFrame {
    private Kernel kernel;
    private JTextArea displayArea;
    
    public ProcessManagementWindow(Kernel kernel) {
        this.kernel = kernel;
        setTitle("Process Management");
        setSize(700, 600);
        setLayout(new BorderLayout(10, 10));
        
        // Button Panel
        JPanel btnPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        addButton(btnPanel, "Create Process", e -> createProcess());
        addButton(btnPanel, "Destroy Process", e -> destroyProcess());
        addButton(btnPanel, "Suspend Process", e -> suspendProcess());
        addButton(btnPanel, "Resume Process", e -> resumeProcess());
        addButton(btnPanel, "Block Process", e -> blockProcess());
        addButton(btnPanel, "Wakeup Process", e -> wakeupProcess());
        addButton(btnPanel, "Change Priority", e -> changePriority());
        addButton(btnPanel, "Schedule FCFS", e -> scheduleFCFS());
        addButton(btnPanel, "Schedule SJF", e -> scheduleSJF());
        addButton(btnPanel, "Schedule RR", e -> scheduleRoundRobin());
        
        // Display Area
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(displayArea);
        
        add(btnPanel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        
        updateDisplay();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void addButton(JPanel panel, String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.addActionListener(action);
        panel.add(btn);
    }
    
    private void createProcess() {
        try {
            // Ask for number of processes
            String numInput = JOptionPane.showInputDialog(this, "Enter number of processes to create:", "1");
            if (numInput == null || numInput.trim().isEmpty()) return;
            
            int numProcesses = Integer.parseInt(numInput);
            if (numProcesses <= 0) {
                JOptionPane.showMessageDialog(this, "Number of processes must be positive!");
                return;
            }
            
            // Create a dialog to input details for each process
            JDialog dialog = new JDialog(this, "Create " + numProcesses + " Processes", true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(600, 400);
            
            JPanel inputPanel = new JPanel(new GridLayout(numProcesses + 1, 4, 5, 5));
            inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Headers
            inputPanel.add(new JLabel("Process #", JLabel.CENTER));
            inputPanel.add(new JLabel("Priority (1-10)", JLabel.CENTER));
            inputPanel.add(new JLabel("Memory (KB)", JLabel.CENTER));
            inputPanel.add(new JLabel("Burst Time", JLabel.CENTER));
            
            // Create input fields for each process
            JTextField[][] fields = new JTextField[numProcesses][3];
            for (int i = 0; i < numProcesses; i++) {
                inputPanel.add(new JLabel("P" + (i + 1), JLabel.CENTER));
                
                fields[i][0] = new JTextField("5");
                fields[i][1] = new JTextField("1024");
                fields[i][2] = new JTextField(String.valueOf(3 + i));
                
                inputPanel.add(fields[i][0]);
                inputPanel.add(fields[i][1]);
                inputPanel.add(fields[i][2]);
            }
            
            JScrollPane scrollPane = new JScrollPane(inputPanel);
            dialog.add(scrollPane, BorderLayout.CENTER);
            
            // Ask for arrival time pattern
            JPanel bottomPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
            
            bottomPanel.add(new JLabel("Arrival Time Pattern:"));
            String[] patterns = {"Same Time (0)", "Sequential (0,1,2,...)", "Custom Interval"};
            JComboBox<String> patternBox = new JComboBox<>(patterns);
            bottomPanel.add(patternBox);
            
            bottomPanel.add(new JLabel("Interval/Start Time:"));
            JTextField intervalField = new JTextField("0");
            bottomPanel.add(intervalField);
            
            dialog.add(bottomPanel, BorderLayout.NORTH);
            
            // Buttons
            JPanel btnPanel = new JPanel();
            JButton createBtn = new JButton("Create All");
            JButton cancelBtn = new JButton("Cancel");
            
            createBtn.addActionListener(e -> {
                try {
                    int pattern = patternBox.getSelectedIndex();
                    int startTime = Integer.parseInt(intervalField.getText());
                    
                    for (int i = 0; i < numProcesses; i++) {
                        int priority = Integer.parseInt(fields[i][0].getText());
                        int memory = Integer.parseInt(fields[i][1].getText());
                        int burst = Integer.parseInt(fields[i][2].getText());
                        
                        int arrivalTime;
                        if (pattern == 0) {
                            arrivalTime = startTime;
                        } else if (pattern == 1) {
                            arrivalTime = startTime + i;
                        } else {
                            arrivalTime = startTime * i;
                        }
                        
                        kernel.createProcess("Process" + (i + 1), priority, memory, burst, arrivalTime);
                    }
                    
                    updateDisplay();
                    dialog.dispose();
                    JOptionPane.showMessageDialog(this, numProcesses + " processes created successfully!");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Invalid input! Please enter valid numbers.");
                }
            });
            
            cancelBtn.addActionListener(e -> dialog.dispose());
            
            btnPanel.add(createBtn);
            btnPanel.add(cancelBtn);
            dialog.add(btnPanel, BorderLayout.SOUTH);
            
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number of processes!");
        }
    }
    
    private void destroyProcess() {
        PCB pcb = selectProcess("Select process to destroy");
        if (pcb != null) {
            kernel.destroyProcess(pcb);
            updateDisplay();
        }
    }
    
    private void suspendProcess() {
        PCB pcb = selectProcess("Select process to suspend");
        if (pcb != null) {
            kernel.suspendProcess(pcb);
            updateDisplay();
        }
    }
    
    private void resumeProcess() {
        PCB pcb = selectProcess("Select process to resume");
        if (pcb != null) {
            kernel.resumeProcess(pcb);
            updateDisplay();
        }
    }
    
    private void blockProcess() {
        PCB pcb = selectProcess("Select process to block");
        if (pcb != null) {
            kernel.blockProcess(pcb);
            updateDisplay();
        }
    }
    
    private void wakeupProcess() {
        PCB pcb = selectProcess("Select process to wakeup");
        if (pcb != null) {
            kernel.wakeupProcess(pcb);
            updateDisplay();
        }
    }
    
    private void changePriority() {
        PCB pcb = selectProcess("Select process");
        if (pcb != null) {
            int newPri = Integer.parseInt(JOptionPane.showInputDialog("New priority:", pcb.getPriority()));
            kernel.changePriority(pcb, newPri);
            updateDisplay();
        }
    }
    
    private void scheduleFCFS() {
        kernel.scheduleFCFS();
        updateDisplay();
    }
    
    private void scheduleSJF() {
        kernel.scheduleSJF();
        updateDisplay();
    }
    
    private void scheduleRoundRobin() {
        kernel.scheduleRoundRobin();
        updateDisplay();
    }
    
    private PCB selectProcess(String title) {
        List<PCB> processes = kernel.getAllProcesses();
        if (processes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No processes available");
            return null;
        }
        Object[] options = processes.toArray();
        return (PCB) JOptionPane.showInputDialog(this, title, "Select Process",
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
    }
    
    private void updateDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PROCESS STATUS ===\n\n");
        
        sb.append("RUNNING: ");
        if (kernel.getRunningProcess() != null) {
            sb.append(kernel.getRunningProcess()).append("\n");
        } else {
            sb.append("None\n");
        }
        
        sb.append("\nREADY QUEUE:\n");
        for (PCB p : kernel.getReadyQueue()) {
            sb.append("  ").append(p).append("\n");
        }
        
        sb.append("\nBLOCKED QUEUE:\n");
        for (PCB p : kernel.getBlockedQueue()) {
            sb.append("  ").append(p).append("\n");
        }
        
        sb.append("\nALL PROCESSES:\n");
        for (PCB p : kernel.getAllProcesses()) {
            sb.append(String.format("  P%d: %s, Owner:%s, Priority:%d, Memory:%dKB, Burst:%d, AT:%d, Pages:%d\n",
                    p.getProcessId(), p.getState(), p.getOwner(), p.getPriority(),
                    p.getMemoryRequirement(), p.getBurstTime(), p.getArrivalTime(), p.getPageNumbers().size()));
        }
        
        displayArea.setText(sb.toString());
    }
}

// Memory Management Window
class MemoryManagementWindow extends JFrame {
    private Kernel kernel;
    private JTextArea displayArea;
    private JTextField maxPagesField;
    
    public MemoryManagementWindow(Kernel kernel) {
        this.kernel = kernel;
        setTitle("Memory Management");
        setSize(700, 600);
        setLayout(new BorderLayout(10, 10));
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        addButton(btnPanel, "Refresh Display", e -> updateDisplay());
        addButton(btnPanel, "Fixed Partitioning", e -> showFixedPartitioning());
        addButton(btnPanel, "Simulate Page Access", e -> { 
            kernel.simulatePageAccess(); 
            updateDisplay(); 
            JOptionPane.showMessageDialog(this, "Simulated random page accesses!");
        });
        
        btnPanel.add(new JLabel("  Max Pages:"));
        maxPagesField = new JTextField("10", 5);
        btnPanel.add(maxPagesField);
        
        JButton lruBtn = new JButton("Apply LRU");
        lruBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        lruBtn.setFocusPainted(false);
        lruBtn.addActionListener(e -> applyLRU());
        btnPanel.add(lruBtn);
        addButton(btnPanel, "Clear All Pages", e -> { 
            kernel.getPageTable().clear(); 
            updateDisplay();
            JOptionPane.showMessageDialog(this, "All pages cleared!");
        });
        
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        displayArea.setBackground(new Color(250, 250, 250));
        
        add(btnPanel, BorderLayout.NORTH);
        add(new JScrollPane(displayArea), BorderLayout.CENTER);
        
        updateDisplay();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void addButton(JPanel panel, String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.PLAIN, 11));
        btn.setFocusPainted(false);
        btn.addActionListener(action);
        panel.add(btn);
    }
    
    private void applyLRU() {
        try {
            int maxPages = Integer.parseInt(maxPagesField.getText());
            String result = kernel.applyLRU(maxPages);
            updateDisplay();
            JOptionPane.showMessageDialog(this, result, "LRU Result", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid max pages value!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("           MEMORY MANAGEMENT SYSTEM\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        sb.append("Page Size: ").append(kernel.getPageSize()).append(" bytes\n");
        sb.append("Total Pages Allocated: ").append(kernel.getPageTable().size()).append("\n\n");
        
        if (kernel.getPageTable().isEmpty()) {
            sb.append("No pages allocated yet.\n");
            sb.append("\nTip: Create processes to allocate pages!\n");
        } else {
            sb.append("PAGE TABLE (Sorted by Last Access Time):\n");
            sb.append("─────────────────────────────────────────────────\n");
            sb.append(String.format("%-8s | %-12s | %-15s | %s\n", "Page#", "Process", "Access Count", "Last Accessed"));
            sb.append("─────────────────────────────────────────────────\n");
            
            // Sort pages by last accessed time
            List<Map.Entry<Integer, Page>> sortedPages = new ArrayList<>(kernel.getPageTable().entrySet());
            sortedPages.sort((a, b) -> Long.compare(a.getValue().getLastAccessed(), b.getValue().getLastAccessed()));
            
            for (Map.Entry<Integer, Page> entry : sortedPages) {
                Page page = entry.getValue();
                long timeDiff = System.currentTimeMillis() - page.getLastAccessed();
                String timeStr = timeDiff < 1000 ? "Just now" : (timeDiff/1000) + "s ago";
                
                sb.append(String.format("%-8d | P%-11d | %-15d | %s\n", 
                        entry.getKey(), 
                        page.getProcessId(), 
                        page.getAccessCount(),
                        timeStr));
            }
            
            sb.append("\n");
            sb.append("LRU INFO:\n");
            sb.append("─────────────────────────────────────────────────\n");
            if (!sortedPages.isEmpty()) {
                Page oldestPage = sortedPages.get(0).getValue();
                sb.append(String.format("Least Recently Used: Page #%d (Process P%d)\n", 
                        sortedPages.get(0).getKey(), oldestPage.getProcessId()));
                
                Page newestPage = sortedPages.get(sortedPages.size() - 1).getValue();
                sb.append(String.format("Most Recently Used: Page #%d (Process P%d)\n", 
                        sortedPages.get(sortedPages.size() - 1).getKey(), newestPage.getProcessId()));
            }
        }
        
        displayArea.setText(sb.toString());
    }
    
    private void showFixedPartitioning() {
        JDialog dialog = new JDialog(this, "Fixed Partitioning (Contiguous)", true);
        dialog.setSize(800, 600);
        dialog.setLayout(new BorderLayout(10, 10));
        
        // Display area for partitions
        JTextArea partitionArea = new JTextArea();
        partitionArea.setEditable(false);
        partitionArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        partitionArea.setBackground(new Color(250, 250, 250));
        
        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        
        JButton allocateBtn = new JButton("Allocate Process");
        allocateBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        allocateBtn.addActionListener(e -> {
            List<PCB> processes = kernel.getAllProcesses();
            if (processes.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "No processes available! Create processes first.");
                return;
            }
            
            Object[] options = processes.toArray();
            PCB selected = (PCB) JOptionPane.showInputDialog(dialog, 
                    "Select process to allocate:", "Allocate Process",
                    JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            
            if (selected != null) {
                String result = kernel.allocateFixedPartition(selected);
                JOptionPane.showMessageDialog(dialog, result);
                updatePartitionDisplay(partitionArea);
            }
        });
        
        JButton deallocateBtn = new JButton("Deallocate Partition");
        deallocateBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        deallocateBtn.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(dialog, "Enter partition ID (1-5):");
            if (input != null && !input.trim().isEmpty()) {
                try {
                    int partId = Integer.parseInt(input);
                    String result = kernel.deallocateFixedPartition(partId);
                    JOptionPane.showMessageDialog(dialog, result);
                    updatePartitionDisplay(partitionArea);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Invalid partition ID!");
                }
            }
        });
        
        JButton resetBtn = new JButton("Reset All Partitions");
        resetBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        resetBtn.addActionListener(e -> {
            kernel.resetPartitions();
            JOptionPane.showMessageDialog(dialog, "All partitions reset!");
            updatePartitionDisplay(partitionArea);
        });
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        refreshBtn.addActionListener(e -> updatePartitionDisplay(partitionArea));
        
        btnPanel.add(allocateBtn);
        btnPanel.add(deallocateBtn);
        btnPanel.add(resetBtn);
        btnPanel.add(refreshBtn);
        
        dialog.add(btnPanel, BorderLayout.NORTH);
        dialog.add(new JScrollPane(partitionArea), BorderLayout.CENTER);
        
        updatePartitionDisplay(partitionArea);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void updatePartitionDisplay(JTextArea area) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════════════\n");
        sb.append("              FIXED PARTITIONING - CONTIGUOUS ALLOCATION\n");
        sb.append("═══════════════════════════════════════════════════════════════════\n\n");
        
        sb.append("PARTITION TABLE:\n");
        sb.append("───────────────────────────────────────────────────────────────────\n");
        sb.append(String.format("%-12s | %-15s | %-10s | %-12s | %s\n", 
                "Partition", "Address Range", "Size (KB)", "Status", "Process"));
        sb.append("───────────────────────────────────────────────────────────────────\n");
        
        int totalAllocated = 0;
        int totalFree = 0;
        int internalFragmentation = 0;
        
        for (Partition p : kernel.getPartitions()) {
            String addressRange = String.format("%d-%d", p.getStartAddress(), p.getEndAddress());
            String status = p.isAllocated() ? "ALLOCATED" : "FREE";
            String process = p.isAllocated() ? "P" + p.getProcessId() : "-";
            
            sb.append(String.format("%-12d | %-15s | %-10d | %-12s | %s\n",
                    p.getId(), addressRange, p.getSize(), status, process));
            
            if (p.isAllocated()) {
                totalAllocated += p.getSize();
                // Find the process to calculate internal fragmentation
                for (PCB pcb : kernel.getAllProcesses()) {
                    if (pcb.getProcessId() == p.getProcessId()) {
                        internalFragmentation += (p.getSize() - pcb.getMemoryRequirement());
                        break;
                    }
                }
            } else {
                totalFree += p.getSize();
            }
        }
        
        sb.append("\n");
        sb.append("MEMORY STATISTICS:\n");
        sb.append("───────────────────────────────────────────────────────────────────\n");
        sb.append(String.format("Total Memory:              1024 KB\n"));
        sb.append(String.format("Allocated Memory:          %d KB\n", totalAllocated));
        sb.append(String.format("Free Memory:               %d KB\n", totalFree));
        sb.append(String.format("Internal Fragmentation:    %d KB\n", internalFragmentation));
        
        int allocatedCount = 0;
        for (Partition p : kernel.getPartitions()) {
            if (p.isAllocated()) allocatedCount++;
        }
        sb.append(String.format("Partitions Allocated:      %d / %d\n", allocatedCount, kernel.getPartitions().size()));
        
        sb.append("\n");
        sb.append("VISUAL REPRESENTATION:\n");
        sb.append("───────────────────────────────────────────────────────────────────\n");
        for (Partition p : kernel.getPartitions()) {
            int blocks = p.getSize() / 20;
            String visual = p.isAllocated() ? "█" : "░";
            sb.append(String.format("P%d [%d KB]: ", p.getId(), p.getSize()));
            for (int i = 0; i < blocks; i++) {
                sb.append(visual);
            }
            if (p.isAllocated()) {
                sb.append(" (Process P" + p.getProcessId() + ")");
            }
            sb.append("\n");
        }
        
        area.setText(sb.toString());
    }
}

// Configuration Window
class ConfigurationWindow extends JFrame {
    private Kernel kernel;
    
    public ConfigurationWindow(Kernel kernel) {
        this.kernel = kernel;
        setTitle("Configuration");
        setSize(400, 300);
        setLayout(new GridLayout(4, 2, 10, 10));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        add(new JLabel("Page Size (bytes):"));
        JTextField pageSizeField = new JTextField(String.valueOf(kernel.getPageSize()));
        add(pageSizeField);
        
        JButton saveBtn = new JButton("Save Configuration");
        saveBtn.addActionListener(e -> {
            try {
                int newPageSize = Integer.parseInt(pageSizeField.getText());
                kernel.setPageSize(newPageSize);
                JOptionPane.showMessageDialog(this, "Configuration saved!");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input!");
            }
        });
        
        add(new JLabel());
        add(saveBtn);
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
}