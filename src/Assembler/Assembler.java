package Assembler;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Assembler {

    private static final String[] opcodesTable = {"STOP", "LOAD", "STORE", "ADD",
        "SUB", "GOTO", "IFZER", "IFNEG"};

    private static final String labelOpRegEx = "^[a-zA-Z]([a-zA-Z0-9]*)$";
    private static final String labelRegEx = "^[a-zA-Z]([a-zA-Z0-9]*:)|:$";
    private static final String addressRegEx = "^[0-9a-fA-F]+(H|h)$";
    private static final String addressOpRegEx = "^\\[[0-9a-fA-F]+(H|h)\\]$";
    private static final String registerRegEx = "^(R|r)[0-3]$";
    private static final String registerOpRegEx = "^\\[(R|r)[0-3]\\]$";
    private static final String immediateRegEx = "^#([0-9a-fA-F]+(H|h)|\\d+)$";
    private static final String dataRegEx = "^([0-9a-fA-F]+(H|h)|\\d+)$";

    private static final int STOP = 0;
    private static final int LOAD = 1;
    private static final int STORE = 2;
    private static final int ADD = 3;
    private static final int SUB = 4;
    private static final int GOTO = 5;
    private static final int IFZER = 6;
    private static final int IFNEG = 7;

    private static final int IMMEDIATE = 0;
    private static final int DIRECT = 1;
    private static final int INDIRECT = 2;

    private int lineCount = 0;
    public HashMap<String, Integer> labels;
    public HashMap<Integer, String> reqLabels;
    public ArrayList<Integer> machineCode;

    public static void main(String[] args) {
        System.out.println("Assembler for the Stallings/TOY1 fake CPU:");
        System.out.println("Assembling test file test-asm-files/load-add-store.asm");
        Assembler asm = new Assembler();
        int[] test;
        try {
            test = asm.assemble(new File("test-asm-files/load-add-store.asm"));
            System.out.println("FINAL OUTPUT");
            int count = 0;
            for (int i : test) {
                System.out.println(String.format("0x%04x - 0x%04x", count, i));
                count++;
            }
        } catch (CompilationError e) {
            e.printStackTrace();
        }
    }

    public Assembler() {
        reset();
    }

    public void reset() {
        labels = new HashMap<String, Integer>();
        reqLabels = new HashMap<Integer, String>();
        machineCode = new ArrayList<Integer>();
    }
    
    public int[] assemble(File f) throws CompilationError {
        reset();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                lineCount++;
                line = line.trim();
                String[] tokens = tokenizeLine(line);

                if (tokens.length > 0) {
                    handleLine(tokens);
                } else {
                    // EMPTY LINE
                    // throw new CompilationError("");
                }
            }

            //check for labels;
            Iterator<Integer> it = reqLabels.keySet().iterator();
            while (it.hasNext()) {
                Integer itInt = it.next();
                Integer address = labels.get(reqLabels.get(itInt.intValue()) + ":");
                int instruction = machineCode.get(itInt.intValue());

                instruction |= address;

                machineCode.remove(itInt.intValue());
                machineCode.add(itInt.intValue(), instruction);

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != br)
                try { 
                    br.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
        }
        
        int[] out = new int[machineCode.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = machineCode.get(i);
        }
        
        return out;
    }
    
    public String[] tokenizeLine(String line) {
        int commentIndex = line.indexOf(';');
        int tokenCount = 0;

        String[] tokens;

        if (commentIndex != -1) {
            line = line.substring(0, commentIndex);
        }

        tokens = line.split(" |,");

        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();
            if (tokens[i].length() != 0) {
                tokens[tokenCount++] = tokens[i];
            }
        }

        String[] out = new String[tokenCount];
        System.arraycopy(tokens, 0, out, 0, tokenCount);
        return out;
    }
    
    public void handleLine(String[] tokens) throws CompilationError {
        if (tokens[0].matches(labelRegEx)) {
            // throw when label exists
            if (!labels.containsKey(tokens[0])) {
                // add new label
                labels.put(tokens[0], machineCode.size());
                
                // build new token list without label
                String[] t = new String[tokens.length - 1];
                System.arraycopy(tokens, 1, t, 0, t.length);
                
                handleOpcode(t);
            } else {
                throw new CompilationError("Label already Defined: " + lineCount + " " + tokens[0]);
            }
        } else {
            handleOpcode(tokens);
        }
    }
        
    private void handleOpcode(String[] t) throws CompilationError {
        int op;

        if (t.length == 0) {
            return;
        }

        if (t[0].equals(opcodesTable[STOP])) {
            if (t.length > 1) {
                throw new CompilationError("Incorrect Number of Operands for Opcode" + " on line " + lineCount);
            } else {
                machineCode.add(new Integer(0));
            }
        } else if (t[0].equals(opcodesTable[GOTO])) {
            if (t.length != 2) {
                throw new CompilationError("Incorrect Number of Operands for Opcode" + " on line " + lineCount);
            } else if (isLabel(t[1])) {
                reqLabels.put(machineCode.size(), t[1]);
                machineCode.add(new Integer(0x5000));
            } else if (isAddress(t[1])) {
                op = handleNumber(t[1]);
                if (op < 0x400) {
                    machineCode.add(new Integer(0x5000 | op));
                } else {
                    throw new CompilationError("Address number too high" + " on line " + lineCount);
                }
            } else {
                throw new CompilationError("Invalid second operand" + " on line " + lineCount);
            }
        } else if (t[0].equals(opcodesTable[IFNEG])) {
            handleIf(t, 0x7);
        } else if (t[0].equals(opcodesTable[IFZER])) {
            handleIf(t, 0x6);
        } else if (t[0].equals(opcodesTable[LOAD])) {
            handleOther(LOAD, t);
        } else if (t[0].equals(opcodesTable[STORE])) {
            handleOther(STORE, t);
        } else if (t[0].equals(opcodesTable[ADD])) {
            handleOther(ADD, t);
        } else if (t[0].equals(opcodesTable[SUB])) {
            handleOther(SUB, t);
        } else if (isData(t[0])) {
            if (t.length != 1)
                throw new CompilationError("Too many data values on line " + lineCount);
            else {
                op = handleNumber(t[0]);
                machineCode.add(new Integer(op));
            }
        } else {
            ;//error
        }
    }

    private void handleOther(int op, String[] t) throws CompilationError {
        int originalOp = op;
        switch (op) {
            case LOAD:
                op = 0x1000;
                break;
            case STORE:
                op = 0x2000;
                break;
            case ADD:
                op = 0x3000;
                break;
            case SUB:
                op = 0x4000;
                break;
        }

        if (t.length != 3) {
            throw new CompilationError("Incorrect Number of Operands for Opcode " + opcodesTable[originalOp] + " on line " + lineCount);
        } else {
            if (isRegister(t[1])) {
                op |= handleReg(t[1]) << 10;
            } else {
                throw new CompilationError("First Operand is not a Register " + t[1] + " on line " + lineCount);//error
            }
            int value;
            if (isAddressOp(t[2])) {
                value = handleDirect(t[2]);
                if (value < 0x400) {
                    machineCode.add(new Integer(op | value));
                } else {
                    throw new CompilationError("Addess number too high " + t[2] + " on line " + lineCount);
                }
            } else if (isLabel(t[2])) {
                reqLabels.put(machineCode.size(), t[2]);
                machineCode.add(new Integer(op));
            } else if (isRegisterOp(t[2])) {
                op |= handleRegOp(t[2]) << 8;
                op &= 0xfff;
                op |= handleOp(LOAD, INDIRECT) << 12;
                machineCode.add(new Integer(op));
            } else if (isImmediate(t[2])) {
                value = handleImmediate(t[2]);
                if (value >= 400) {
                    throw new CompilationError("Immediate Value is too high " + t[2] + " on line " + lineCount);
                } else {
                    op |= value;
                    op &= 0xfff;
                    op |= handleOp(LOAD, IMMEDIATE) << 12;
                    machineCode.add(new Integer(op));
                }
            } else {
                throw new CompilationError("Second Operand not Valid for Opcode " + opcodesTable[originalOp] + " on line " + lineCount);
            }
        }
    }

    private int handleOp(int op, int addressing) {
        switch (addressing) {
            case IMMEDIATE:
                switch (op) {
                    case LOAD:
                        return 12;
                    case STORE:
                        return 13;
                    case ADD:
                        return 14;
                    case SUB:
                        return 15;
                }
                break;
            case INDIRECT:
                switch (op) {
                    case LOAD:
                        return 8;
                    case STORE:
                        return 9;
                    case ADD:
                        return 10;
                    case SUB:
                        return 11;
                }
                break;
        }
        return 16;
    }

    private int handleDirect(String operand) {
        operand = operand.split("\\[|\\]")[1];
        return handleNumber(operand);
    }

    private int handleImmediate(String operand) {
        operand = operand.substring(1);
        return handleNumber(operand);
    }

    private int handleNumber(String operand) {
        if (operand.charAt(operand.length() - 1) == 'H' | operand.charAt(operand.length() - 1) == 'h') {
            operand = operand.substring(0, operand.length() - 1);
            return Integer.parseInt(operand, 16);
        }
        return Integer.parseInt(operand);
    }

    private void handleIf(String[] t, int op) throws CompilationError {
        op <<= 12;
        if (t.length != 3) {
            throw new CompilationError("Incorrect number of operands for opcode" + " on line " + lineCount);
        } else {
            if (isRegister(t[1])) {
                op |= handleReg(t[1]) << 10;
            } else {
                throw new CompilationError("First Operand is not a Register" + " on line " + lineCount);
            }
            if (isAddress(t[2])) {
                if (Integer.parseInt(t[2].substring(0, t[2].length() - 1), 16) < 0x400) {
                    machineCode.add(new Integer(op | Integer.parseInt(t[2].substring(0, t[2].length() - 1), 16)));
                } else {
                    throw new CompilationError("Address Value Too High" + " on line " + lineCount);
                }
            } else if (isLabel(t[2])) {
                reqLabels.put(machineCode.size(), t[2]);
                machineCode.add(new Integer(op));
            } else {
                throw new CompilationError("Not valid second operand" + " on line " + lineCount);
            }
        }
    }

    private boolean isData(String operand) {
        return operand.matches(dataRegEx);
    }

    private boolean isImmediate(String operand) {
        return operand.matches(immediateRegEx);
    }

    private int handleRegOp(String operand) {
        return handleReg(operand.split("\\[|\\]")[1]);
    }

    private int handleReg(String operand) {
        return Integer.parseInt(operand.charAt(1) + "");
    }

    private boolean isRegisterOp(String operand) {
        return operand.matches(registerOpRegEx);
    }

    private boolean isRegister(String operand) {
        return operand.matches(registerRegEx);
    }

    private boolean isLabel(String operand) {
        return operand.matches(labelOpRegEx);
    }

    private boolean isAddress(String operand) {
        return operand.matches(addressRegEx);
    }

    private boolean isAddressOp(String operand) {
        return operand.matches(addressOpRegEx);
    }    
}
