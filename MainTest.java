package gitlet;

import org.junit.Test;
import ucb.junit.textui;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * The suite of all JUnit tests for the gitlet package.
 *
 * @author
 */
public class MainTest {

    /**
     * Run the JUnit tests in the loa package. Add xxxTest.class entries to
     * the arguments of runClasses to run other JUnit tests.
     */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /**
     * A dummy test to avoid complaint.
     */
    private static String _repoDir;
    private static String _commitDir;

    @Test
    public void initTest() {
        File file = new File(".");
        UUID iD = UUID.randomUUID();
        String dir = iD.toString();
        _repoDir = new File(dir, ".gitlet")
                .getAbsolutePath();
        _commitDir = new File(_repoDir, ".commit")
                .getAbsolutePath();
        File testFolder = new File(dir);
        assertEquals(true, testFolder.mkdir());
        Main.init(testFolder.getAbsolutePath());
        assertFileExists(dir, ".gitlet", true);
        assertFileExists(_repoDir, "master.branch",
                false);
        assertFileExists(_repoDir,
                "add.staging",
                false);
        assertFileExists(_repoDir, "remove.staging",
                false);
        assertFileExists(_repoDir, ".commit",
                true);
        Branch master = new Branch(new File(_repoDir,
                "master.branch"));
        assertEquals("master", master.getName());
        assertFileExists(_commitDir, master.getHead(),
                false);
        Commit testStoredCommit =
                new Commit(new File(_commitDir, master.getHead()));
        assertEquals(null,
                testStoredCommit.getParentCommit());
        assertEquals("initial commit",
                testStoredCommit.getMessage());
        assertEquals(master.getHead(),
                Utils.sha1(Utils.serialize(testStoredCommit)));
        assertFileExists(_repoDir, "HEAD",
                false);
    }

    @Test
    public void initTestFail() {
        File file = new File(".");
        UUID iD = UUID.randomUUID();
        String dir = iD.toString();
        String repoDir =
                new File(dir, ".gitlet").getAbsolutePath();
        File testFolder = new File(dir);
        assertEquals(true, testFolder.mkdir());
        Main.init(testFolder.getAbsolutePath());
        try {
            Main.init(testFolder.getAbsolutePath());
            assertFalse(true);
        } catch (GitletException e) {
            return;
        }
    }

    @Test
    public void addTest() {
        initTest();
        File dir = new File(_repoDir);
        File workingDir = new File(_repoDir
                .replace(".gitlet", ""));
        if (!workingDir.exists()) {
            assertTrue(false);
        }
        File newFileToAdd = new File(workingDir
                .getAbsolutePath(), "Hello.txt");
        Utils.writeObject(newFileToAdd, "Hello.txt");
        Main.add(workingDir.getAbsolutePath(), "Hello.txt");
    }

    @Test
    public void commitTest() {
        addTest();
        File dir = new File(_repoDir);
        File workingDir = new File(_repoDir
                .replace(".gitlet", ""));
        Main.commit(workingDir.getAbsolutePath(),
                "Hello World!");
        Main.log(workingDir.getAbsolutePath());
    }

    public void assertHashMapEquality(HashMap<String,
            String> expected, HashMap<String, String> actual) {
        for (String str : expected.keySet()) {
            assertEquals(expected.get(str), actual.get(str));
        }
        for (String str : actual.keySet()) {
            assertEquals(expected.get(str), actual.get(str));
        }
    }

    @Test
    public void checkoutFileFromHeadTest() {
        addTest();
        File dir = new File(_repoDir);
        File workingDir = new File(_repoDir
                .replace(".gitlet", ""));
        Main.commit(workingDir.getAbsolutePath(),
                "Hello World!");
        Main.log(workingDir.getAbsolutePath());
        Main.checkoutFileFromHEADtoWorkingDir(workingDir
                .getAbsolutePath(), "Hello.txt");
    }

    @Test
    public void checkoutFileFromPrevCommit() {
        addTest();
        File dir = new File(_repoDir);
        File workingDir = new File(_repoDir
                .replace(".gitlet", ""));
        Main.commit(workingDir.getAbsolutePath(),
                "Hello World!");
        Main.log(workingDir.getAbsolutePath());
        Commit mostRecentCommit = Commit.getHeadCommit(
                workingDir.getAbsolutePath(),
                Branch.getCurrentBranch(workingDir.getAbsolutePath()));
        Main.checkoutFileFromCommitToWorkingDir(
                workingDir.getAbsolutePath(),
                mostRecentCommit.getHash(), "Hello.txt");
    }

    @Test
    public void checkoutFromCommit() {
        initTest();
        File dir = new File(_repoDir);
        File workingDir = new File(_repoDir
                .replace(".gitlet", ""));
        File helloWorld =
                new File(workingDir.getAbsolutePath(),
                        "Hello.txt");
        String toWriteInitially =
                "Hello world Please Start Working";
        Utils.writeContents(helloWorld, toWriteInitially);
        Main.add(workingDir.getAbsolutePath(),
                "Hello.txt");
        Main.commit(workingDir.getAbsolutePath(),
                "Added Hello.txt");
        Commit mostRecentCommit = Commit
                .getHeadCommit(workingDir.getAbsolutePath(),
                        Branch.getCurrentBranch(workingDir
                                .getAbsolutePath()));
        String newChanges = "Things have been changed";
        Utils.writeContents(helloWorld, newChanges);
        Main.checkoutFileFromCommitToWorkingDir(
                workingDir.getAbsolutePath(),
                mostRecentCommit.getHash(),
                "Hello.txt");
        String changesAgain = "Changing things once again";
        Utils.writeContents(helloWorld, changesAgain);
        Main.add(workingDir.getAbsolutePath(),
                "Hello.txt");
        Main.commit(workingDir.getAbsolutePath(),
                "Made my second commit after initial");
        Main.log(workingDir.getAbsolutePath());
        Main.checkoutFileFromCommitToWorkingDir(
                workingDir.getAbsolutePath(),
                mostRecentCommit.getHash(), "Hello.txt");
        Main.makeNewBranch(workingDir.getAbsolutePath(),
                "branch1");
        File newBranch = new File(_repoDir, "branch1.branch");
        Branch newBranchFromFile = new Branch(newBranch);
        Branch master = Branch.getCurrentBranch(
                workingDir.getAbsolutePath());
    }

    @Test
    public void checkoutFromBranch() {
        initTest();
        File dir = new File(_repoDir);
        File workingDir = new File(_repoDir.replace(
                ".gitlet", ""));
        File helloFile = new File(workingDir.getAbsolutePath(),
                "Hello.txt");
        Utils.writeContents(helloFile,
                "Initial hello txt file");
        Main.add(workingDir.getAbsolutePath(),
                "Hello.txt");
        Main.commit(workingDir.getAbsolutePath(),
                "made Hello.txt and wrote contents");
        Main.makeNewBranch(workingDir.getAbsolutePath(),
                "branch1");
        Utils.writeContents(helloFile,
                "Changing contents after creating new branch1");
        Main.add(workingDir.getAbsolutePath(),
                "Hello.txt");
        Main.commit(workingDir.getAbsolutePath(),
                "adding updated Hello.txt");
        Main.checkoutBranch(workingDir.getAbsolutePath(),
                "branch1");
        Main.checkoutBranch(workingDir.getAbsolutePath(),
                "master");
        Main.remove(workingDir.getAbsolutePath(),
                "Hello.txt");
        Main.checkoutBranch(workingDir.getAbsolutePath(),
                "branch1");
        Main.checkoutBranch(workingDir.getAbsolutePath(),
                "master");
        Main.remove(workingDir.getAbsolutePath(),
                "Hello.txt");
        Main.commit(workingDir.getAbsolutePath(),
                "Now commiting after having removed");
        Main.checkoutBranch(workingDir.getAbsolutePath(),
                "branch1");
        Main.checkoutBranch(workingDir.getAbsolutePath(),
                "master");
        Main.checkoutBranch(workingDir.getAbsolutePath(),
                "branch1");
    }

    @Test
    public void emptyCommit() {
        initTest();
        File dir = new File(_repoDir);
        File workingDir = new File(_repoDir
                .replace(".gitlet", ""));
        try {
            Main.commit(workingDir.getAbsolutePath(),
                    "Empty Commit Should Fail");
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void makeBranch() {
        checkoutFromCommit();
        File dir = new File(_repoDir);
        File workingDir = new File(_repoDir
                .replace(".gitlet", ""));
        try {
            Main.makeNewBranch(workingDir
                    .getAbsolutePath(), "branch1");
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }

    }

    public void assertFileExists(String parent,
                                 String fileName, boolean testDir) {
        File testFile = new File(parent, fileName);
        assertTrue(testFile.exists());
        if (testDir) {
            assertTrue(testFile.isDirectory());
        } else {
            assertFalse(testFile.isDirectory());
        }
    }

    @Test
    public void checkStatus() {
        makeBranch();
        File dir = new File(_repoDir);
        File workingDir = new File(_repoDir
                .replace(".gitlet", ""));
        Main.log(workingDir.getAbsolutePath());
        Main.status(workingDir.getAbsolutePath());
        Main.add(workingDir.getAbsolutePath(),
                "Hello.txt");
        Main.status(workingDir.getAbsolutePath());
        Main.remove(workingDir.getAbsolutePath(),
                "Hello.txt");
        Main.status(workingDir.getAbsolutePath());
        Main.removeBranch(workingDir.getAbsolutePath(),
                "branch1");
        Main.status(workingDir.getAbsolutePath());
        Main.globalLog(workingDir.getAbsolutePath());
        Main.findFromMessage(workingDir.getAbsolutePath(),
                "Made my second commit after initial");
    }

    @Test
    public void checkMerge() {
        initTest();
        File dir = new File(_repoDir);
        File workingDir = new File(_repoDir
                .replace(".gitlet", ""));
        Main.makeNewBranch(workingDir.getAbsolutePath(),
                "given");
        File f = writeContentsToFileNameInWorkingDir
                (workingDir, "f.txt", "wug.txt");
        Main.add(workingDir.getAbsolutePath(),
                "f.txt");
        Main.commit(workingDir.getAbsolutePath(),
                "Add f.txt containing wug.txt");
        Main.checkoutBranch(workingDir.getAbsolutePath(),
                "given");
        Utils.writeContents(f, "notwug.txt\n");
        Main.add(workingDir.getAbsolutePath(), "f.txt");
        Main.commit(workingDir.getAbsolutePath(),
                "Add f.txt containing notwug.txt");
        Main.makeNewBranch(workingDir.getAbsolutePath(),
                "B");
        Main.merge(workingDir.getAbsolutePath(),
                "master");
    }

    public File writeContentsToFileNameInWorkingDir(File workingDir,
                                                    String fileName,
                                                    String contents) {
        if (!workingDir.isDirectory()) {
            throw new GitletException("directory doesn't exist");
        } else {
            File newFile = new File(workingDir.getAbsolutePath(), fileName);
            Utils.writeContents(newFile, contents);
            return newFile;
        }
    }
}


