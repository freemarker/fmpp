/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fmpp.testsuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import fmpp.tools.CommandLine;
import fmpp.util.FileUtil;
import fmpp.util.MiscUtil;
import fmpp.util.StringUtil;

/**
 * FMPP test suite.
 */
public class TestSuite {
    private final File projsParentDir;
    private final File refsParentDir;
    private final File suiteOutputDir;
    private final String testcase;
    private Writer suiteLogWriter;
    private TestProgressListener progressListener;
     
    private int countSuccessfull; 
    private int countDiffers;
    private int countError;
    private List differProjectNames = new ArrayList();
    private List errorProjectNames = new ArrayList();
    
    /**
     * Commandline interface for the test suite.
     * 
     * <p>Command-line parameters:
     * <ol>
     *   <li>FMPP home directory. Required.
     *   <li>The name of the testcase to run. If omitted or empty string,
     *       then all testcases will be executed.
     * <ol>
     */
    public static void main(String[] args) throws Exception {
        int argsln = args.length;
        if (argsln != 1 && argsln != 2) {
            System.out.println(
                    "Usage: java fmpp.testsuite.TestSuite "                    + "<fmpp_home_dir> [testcase]");
            System.out.println();
            System.exit(-1);
        }
        File fmppHomeDir = new File(args[0]);
        if (!fmppHomeDir.isDirectory()) {
            System.out.println(
                    "The specified FMPP home directory does not exist.");
            System.out.println();
            System.exit(-1);
        }
        File outputDir = new File(
                fmppHomeDir, "build" + File.separator + "test-output");
        String testcase = null;
        if (argsln > 1) {
            testcase = args[1];
            if (testcase.length() == 0) {
                testcase = null;
            }
        }
        TestSuite testSuite = new TestSuite(
                new File(fmppHomeDir,
                        "src" + File.separator + "test" + File.separator + "resources"
                        + File.separator + "tests"),
                new File(fmppHomeDir,
                        "src" + File.separator + "test" + File.separator + "resources"
                        + File.separator + "expected"),
                outputDir,
                testcase);
        testSuite.progressListener = new ConsoleTestProgressListener(); 
        testSuite.run();
        System.out.println("=================================================");
        System.out.println("OK: " + testSuite.countSuccessfull
                + ", Differs: " + testSuite.countDiffers
                + ", Error: " + testSuite.countError);
        System.out.println();
        if (testSuite.countError != 0 || testSuite.countDiffers != 0) {
            System.out.println("/\\/\\/ TEST SUITE FAILED! \\/\\/\\");
            if (testSuite.countError != 0) {
                System.out.println("Projects failed with errors were:");
                for (int i = 0; i < testSuite.errorProjectNames.size(); i++) {
                    System.out.print("- ");
                    System.out.println(testSuite.errorProjectNames.get(i));
                }
            }
            if (testSuite.countDiffers != 0) {
                System.out.println(
                        "Projects where the output and the reference differs:");
                for (int i = 0; i < testSuite.differProjectNames.size(); i++) {
                    System.out.print("- ");
                    System.out.println(testSuite.differProjectNames.get(i));
                }
            }
            System.out.println("");
            System.out.println("Look into build/test-output/ for more details.");
            System.exit(-1);
        } else {
            System.out.println("*** TEST SUITE SUCCESSFULL ***");
            System.exit(0);
        }
    }
    
    public TestSuite(
            File projsParentDir, File refsParentDir, File suiteOutputDir,
            String testcase)
            throws IOException {
        this.projsParentDir = projsParentDir;
        this.refsParentDir = refsParentDir;
        this.suiteOutputDir = suiteOutputDir;
        this.testcase = testcase;
    }

    public void run() throws IOException {
        File[] projects;
        if (testcase == null) {
            projects = removeCVSAndSVNFiles(projsParentDir.listFiles());
        } else {
            projects = new File[] {new File(projsParentDir, testcase)};
        }
        if (projects == null) {
            throw new FileNotFoundException("Project directory not found: "
                    + projsParentDir.getCanonicalPath());
        }
        createEmptyDir(suiteOutputDir, "compile");
        this.suiteLogWriter = new FileWriter(
                new File(suiteOutputDir, "suitelog.txt"));
        try {
            for (int i = 0; i < projects.length; i++) {
                if (projects[i].isDirectory()) {
                    String projName = projects[i].getName();
                    File projOutDir = new File(
                            suiteOutputDir,
                            "projects" + File.separator + projName);
                    createEmptyDir(projOutDir, null);
                    String[] args = new String[] {
                            "-C", projects[i].getAbsolutePath(),
                            "-O", new File(projOutDir, "out")
                                    .getAbsolutePath(),
                            "-L", new File(projOutDir, "projlog.txt")
                                    .getAbsolutePath(),
                            "--columns", "76",
                            "-q"};
                    File argsFile = new File(projects[i], "args.txt");
                    if (argsFile.exists()) {
                        String[] oldArgs = args;
                        String[] extraArgs = loadArgs(argsFile);
                        args = new String[
                                oldArgs.length + extraArgs.length];
                        int x;
                        for (x = 0; x < oldArgs.length; x++) {
                            args[x] = oldArgs[x];
                        }
                        int y = 0;
                        for (; x < args.length; x++) {
                            args[x] = extraArgs[y];
                            y++;
                        }
                    }
                    StringWriter capturedStderr = new StringWriter();
                    PrintWriter pwr = new PrintWriter(capturedStderr);
                    int exitCode = CommandLine.execute(args, pwr, pwr);
                    if (exitCode != 0) {
                        pwr.close();
                        log(projName + ": Error!");
                        logIndented(
                                StringUtil.chomp(
                                        capturedStderr.toString()));
                        countError++;
                        errorProjectNames.add(projName);
                        if (progressListener != null) {
                            progressListener.sendMessage(
                                    "- " + projName + ": Error!");
                        }                    
                    } else {
                        String diffs = checkProjectOutput(
                                    new File(refsParentDir, projName),
                                    new File(projOutDir, "out"));
                        if (diffs == null) {
                            log(projName + ": OK");
                            countSuccessfull++;
                            if (progressListener != null) {
                                progressListener.sendMessage(
                                        "- " + projName + ": OK");
                            }
                        } else {
                            log(projName + ": Differs!");
                            logIndented(diffs);
                            differProjectNames.add(projName);
                            countDiffers++;
                            if (progressListener != null) {
                                progressListener.sendMessage(
                                        "- " + projName + ": Differs!");
                            }
                        }
                    }
                } else if (!projects[i].exists()) {
                    throw new FileNotFoundException(projects[i].getPath());
                }
            }
        } catch (IOException e) {
            log("Test suite aborted with I/O error: ");
            log(MiscUtil.causeTrace(e));
            throw e;
        } finally {
            this.suiteLogWriter.close();
            this.suiteLogWriter = null;
        }
    }
    
    private static void createEmptyDir(File dir, String keepDir)
            throws IOException {
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            emptyDir(dir, keepDir);
        }
    }
    
    private static void emptyDir(File dir, String keepDir) throws IOException {
        String path = dir.getCanonicalPath();
        if (path.indexOf(File.separator + "build" + File.separator) == -1) {
            throw new IOException("Safety restriction: "
                    + "The directory to empty must be inside directory "
                    + "\"build\". This does not stand for this path: " + path);
        }
        
        File[] files = removeCVSAndSVNFiles(dir.listFiles());
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                if (keepDir == null || !files[i].getName().equals(keepDir)) {
                    emptyDir(files[i], null);
                    files[i].delete();
                }
            } else {
                files[i].delete();
            }
        }
    }
    
    /**
     * Compares two directories and its content for equality.
     * @return <code>null</code> if the two directory is identical, otherwise
     *     the text that describes the difference.
     */
    private String checkProjectOutput(
            File currentProjRefDir, File currentProjOutputDir)
            throws IOException {
        ComparsionEnvironment env = new ComparsionEnvironment(); 
        env.currentProjOutputDir = currentProjOutputDir;
        env.currentProjRefDir = currentProjRefDir;
        checkProjectOutput_compareDirs(
                env, currentProjRefDir, currentProjOutputDir);
        StringUtil.chomp(env.diffs);
        return env.diffs.length() == 0 ? null : env.diffs.toString();
    }
    
    private void checkProjectOutput_compareDirs(
            ComparsionEnvironment env, File refDir, File chkedDir)
            throws IOException {
        File[] refDirFiles = refDir.listFiles();
        if (refDirFiles == null) {
            throw new IOException("Reference directory not found: "
                    + refDir.getAbsolutePath());
        } 
        refDirFiles = removeCVSAndSVNFiles(refDirFiles);
        
        File[] chkedDirFiles = chkedDir.listFiles();
        if (chkedDirFiles == null) {
            throw new IOException("Checked directory not found: "
                    + chkedDir.getAbsolutePath());
        } 
        chkedDirFiles = removeCVSAndSVNFiles(chkedDirFiles);
        
        for (int ci = 0; ci < chkedDirFiles.length; ci++) {
            File cf = chkedDirFiles[ci];
            boolean isDir = cf.isDirectory(); 
            int ri = findCounterpart(refDirFiles, cf);
            if (ri != -1) {
                if (isDir) {
                    checkProjectOutput_compareDirs(env, refDirFiles[ri], cf);
                } else {
                    checkProjectOutput_compareFiles(env, refDirFiles[ri], cf);
                }
                refDirFiles[ri] = null;
            } else {
                env.logDiff(
                        FileUtil.getRelativePath(env.currentProjOutputDir, cf)
                        + ": "
                        + (isDir ? "Directory" : "File")
                        + " shouldn't exist.");
            }
        }
        
        for (int ri = 0; ri < refDirFiles.length; ri++) {
            File rf = refDirFiles[ri];
            if (rf != null) {
                env.logDiff(
                        FileUtil.getRelativePath(env.currentProjRefDir, rf)
                        + ": "
                        + (rf.isDirectory() ? "Directory" : "File")
                        + " is missing.");
            }
        }        
    }

    private void checkProjectOutput_compareFiles(
            ComparsionEnvironment env, File refFile, File chkdFile)
            throws IOException {
        InputStream in;
        
        byte[] ref;
        int refln;
        int refi;
        in = new FileInputStream(refFile);
        try {
            ref = FileUtil.loadByteArray(in);
        } finally {
            in.close();
        }
        refln = ref.length;
        refi = 0;

        byte[] chkd;
        int chkdln;
        int chkdi;
        in = new FileInputStream(chkdFile);
        try {
            chkd = FileUtil.loadByteArray(in);
        } finally {
            in.close();
        }
        chkdln = chkd.length;
        chkdi = 0;

        int refc;
        int chkdc;
        readLoop: do {
            if (refi < refln) {
                refc = ref[refi++];
                if (refc == 0xD) {
                    refc = 0xA;
                    if (refi < refln) {
                        if (ref[refi] == 0xA) {
                            refi++;
                        }
                    }
                }
            } else {
                refc = -1;
            }

            
            if (chkdi < chkdln) {
                chkdc = chkd[chkdi++];
                if (chkdc == 0xD) {
                    chkdc = 0xA;
                    if (chkdi < chkdln) {
                        if (chkd[chkdi] == 0xA) {
                            chkdi++;
                        }
                    }
                }
            } else {
                chkdc = -1;
            }
            
            if (refc != chkdc) {
                if (refc == -1) {
                    env.logDiff(
                            FileUtil.getRelativePath(
                                    env.currentProjRefDir, refFile)
                            + ": The file is longer than the reference file.");
                    break readLoop;
                } else if (chkdc == -1) {
                    env.logDiff(
                            FileUtil.getRelativePath(
                                    env.currentProjRefDir, refFile)
                            + ": The file is shorter than the reference file.");
                    break readLoop;
                } else {
                    env.logDiff(
                            FileUtil.getRelativePath(
                                    env.currentProjRefDir, refFile)
                            + ": Different byte(s) at output file position "
                            + (chkdi));
                    break readLoop;
                }
            }
        } while (!(refc == -1 && chkdc == -1));
    }
    
    private static int findCounterpart(File[] files, File file) {
        String fName = file.getName();
        for (int i = 0; i < files.length; i++) {
            if (files[i] != null && files[i].getName().equals(fName)
                    && (files[i].isDirectory() == file.isDirectory())) {
                return i;
            }
        }
        return -1;
    }
    
    private void log(String text) throws IOException {
        suiteLogWriter.write(StringUtil.wrap(text, 80));
        suiteLogWriter.write(StringUtil.LINE_BREAK);
    }

    private void logIndented(String text) throws IOException {
        suiteLogWriter.write(StringUtil.wrap(text, 80, 4));
        suiteLogWriter.write(StringUtil.LINE_BREAK);
    }
    
    private class ComparsionEnvironment {
        StringBuffer diffs = new StringBuffer();
        File currentProjOutputDir;
        File currentProjRefDir;
        byte[] wb1 = new byte[8192]; 
        byte[] wb2 = new byte[8192];
        
        void logDiff(String message) {
            diffs.append(message);
            diffs.append(StringUtil.LINE_BREAK);
        } 
    }
    
    private String[] loadArgs(File f) throws IOException {
        String s = FileUtil.loadString(new FileInputStream(f), "UTF-8");
        int ln = s.length();
        List ls = new ArrayList();
        int next = 0;
        for (int i = 0; i <= ln; i++) {
            char c;
            if (i != ln) {
                c = s.charAt(i);
            } else {
                c = 0xA;
            }
            if (c == 0xA || c == 0xD) {
                String s2 = s.substring(next, i); 
                next = i + 1;
                s2 = s2.trim();
                if (s2.length() != 0 && !s2.startsWith("#")) {
                    ls.add(s2);
                }
            }
        }
        ln = ls.size();
        String[] res = new String[ln];
        for (int i = 0; i < ln; i++) {
            res[i] = (String) ls.get(i);
        }
        return res;
    }
    
    private static File[] removeCVSAndSVNFiles(File[] files) {
        int ln = files.length;
        ArrayList list = new ArrayList(ln);
        boolean changed = false;
        for (int i = 0; i < ln; i++) {
            String name = files[i].getName().toLowerCase();
            if (!name.equals("cvs") && !name.startsWith(".#")
                    && !name.equals(".svn") && !name.equals(".gitignore")) {
                list.add(files[i]);
            } else {
                changed = true;
            }
        }
        if (changed) {
            ln = list.size();
            files = new File[ln];
            for (int i = 0; i < ln; i++) {
                files[i] = (File) list.get(i);
            }
        }
        return files;
    }
}