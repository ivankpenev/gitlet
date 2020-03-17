package gitlet;

import java.io.File;
import java.io.FilenameFilter;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author ivanp
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {
        String[] commands
                = new String[]{"init", "add", "rm", "comm"
                        + "it", "log", "checkout", "branch", "find", "gl"
                        + "obal-log", "status", "rm-branch", "reset", "merge"};
        Set<String> set = new HashSet<>(Arrays.asList(commands));
        try {
            File testGitletFileExistance = new File(workingDir(),
                    ".gitlet");
            if (args.length == 0) {
                throw new GitletException("Please enter a command.");
            } else if (!set.contains(args[0])) {
                throw new GitletException("No "
                        + "command with that name exists.");
            } else if (!args[0].equals("init")
                    && (!testGitletFileExistance.exists()
                    || !testGitletFileExistance.isDirectory())) {
                throw new GitletException("Not "
                        + "in an initialized Gitlet directory.");
            } else if (args[0].equals("init") && args.length == 1) {
                init(workingDir());
            } else if (args[0].equals("add") && args.length == 2) {
                add(workingDir(), args[1]);
            } else if (args[0].equals("rm") && args.length == 2) {
                remove(workingDir(), args[1]);
            } else if (args[0].equals("commit") && args.length == 2) {
                commit(workingDir(), args[1]);
            } else if (args[0].equals("log")) {
                log(workingDir());
            } else if (args[0].equals("checkout")
                    && args[1].equals("--") && args.length == 3) {
                checkoutFileFromHEADtoWorkingDir(workingDir(), args[2]);
            } else if (args[0].equals("checkout")
                    && args.length == 4 && args[2].equals("--")) {
                checkoutFileFromCommitToWorkingDir(workingDir(),
                        args[1], args[3]);
            } else if (args[0].equals("checkout") && args.length == 2) {
                checkoutBranch(workingDir(), args[1]);
            } else if (args[0].equals("branch") && args.length == 2) {
                makeNewBranch(workingDir(), args[1]);
            } else if (args[0].equals("find") && args.length == 2) {
                findFromMessage(workingDir(), args[1]);
            } else if (args[0].equals("global-log") && args.length == 1) {
                globalLog(workingDir());
            } else if (args[0].equals("status") && args.length == 1) {
                status(workingDir());
            } else if (args[0].equals("rm-branch") && args.length == 2) {
                removeBranch(workingDir(), args[1]);
            } else if (args[0].equals("reset") && args.length == 2) {
                resetToCommit(workingDir(), args[1]);
            } else if (args[0].equals("merge") && args.length == 2) {
                merge(workingDir(), args[1]);
            } else {
                throw new GitletException("Incorrect operands.");
            }
        } catch (GitletException e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }
    }

    /**
     * a.
     *
     * @return string
     */
    static String workingDir() {
        return System.getProperty("user.dir");
    }

    /**
     * sor.
     *
     * @param workingDir yup
     */
    static void init(String workingDir) {
        File repo = new File(workingDir, ".gitlet");
        if (repo.exists()) {
            throw new GitletException("A Gitlet "
                    + "version-control system already exists"
                    + " in the current directory.");
        } else {
            if (!repo.mkdir()) {
                throw new GitletException("Unable "
                        + "to create a repository. Possibly "
                        + "no permission to create a folder.");
            }
            File commitDirectory =
                    new File(repo.getAbsolutePath(), ".commit");
            commitDirectory.mkdir();
            ChangeList add =
                    new ChangeList("add");
            ChangeList remove =
                    new ChangeList("remove");
            add.writeToRepo(workingDir);
            remove.writeToRepo(workingDir);
            Commit firstCommit = new Commit("initial commit",
                    null, repo.getAbsolutePath());
            String firstCommitHash = firstCommit.writeToRepo(workingDir);
            Branch master = new Branch("master", firstCommitHash);
            master.writeToRepo(workingDir);
            String head = master.getName();
            File headFile = new File(repo.getAbsolutePath(), "HEAD");
            Utils.writeContents(headFile, head);
        }
    }

    /**
     * yup.
     *
     * @param workingDir a
     * @param fileName   a
     */
    static void add(String workingDir, String fileName) {
        ChangeList additions
                = ChangeList.readFromRepo(workingDir, "add");
        ChangeList removals
                = ChangeList.readFromRepo(workingDir, "remove");
        File repo = new File(workingDir, ".gitlet");
        Branch currentBranch
                = Branch.getCurrentBranch(workingDir);
        Commit currentCommit
                = Commit.getHeadCommit(workingDir, currentBranch);
        File workingFile = new File(workingDir, fileName);
        if (!workingFile.exists()) {
            throw new GitletException("File does not exist.");
        }
        String sha1Hash = Utils.sha1(workingFile);
        if (removals.containsKey(fileName)) {
            removals.removeFile(workingDir, fileName);
        }
        if (currentCommit.getBlobs().get(fileName) == null
                || !currentCommit.getBlobs().get(fileName).equals(sha1Hash)) {
            additions.put(fileName, sha1Hash);
            additions.writeToRepo(workingDir);
            File target = new File(repo.getAbsolutePath(), sha1Hash);
            if (!target.exists()) {
                Utils.copy(workingFile, target);
            }
        }
    }

    /**
     * yup.
     *
     * @param workingDir a
     * @param message    a
     */
    static void commit(String workingDir, String message) {
        if (message.equals("")) {
            throw new GitletException("Please enter a commit message.");
        }
        File repo = new File(workingDir, ".gitlet");
        File commitDir = new File(repo.getAbsolutePath(), ".commit");
        ChangeList toAdd = Utils.readObject(new File(repo.getAbsolutePath(),
                "add.staging"), ChangeList.class);
        ChangeList toRem = Utils.readObject(new File(repo.getAbsolutePath(),
                "remove.staging"), ChangeList.class);
        if (toAdd.size() + toRem.size() == 0) {
            throw new GitletException("No changes added to the commit.");
        } else {
            Branch workingBranch = Branch.getCurrentBranch(workingDir);
            Commit newCommit = Commit.getHeadCommit(workingDir, workingBranch);
            newCommit.assignParent(workingBranch.getHead());
            newCommit.assignMessage(message);
            newCommit.updateNewDate();
            newCommit.applyAdditions(workingDir);
            newCommit.applyRemovals(workingDir);
            String newHead = newCommit.writeToRepo(workingDir);
            workingBranch.setHead(newHead);
            workingBranch.writeToRepo(workingDir);
        }
    }

    /**
     * yup.
     *
     * @param workingDir a
     * @param fileName   a
     */
    static void checkoutFileFromHEADtoWorkingDir(String workingDir,
                                                 String fileName) {
        File repo = new File(workingDir, ".gitlet");
        Branch currentBranch = Branch.getCurrentBranch(workingDir);
        Commit currentCommit = Commit.getHeadCommit(workingDir, currentBranch);
        String blobHash = currentCommit.getBlobs().get(fileName);
        if (blobHash == null) {
            throw new GitletException("File does not exist in that commit.");
        } else {
            File blob = new File(repo.getAbsolutePath(), blobHash);
            File toBeWrittenInWorkingDir = new File(workingDir, fileName);
            Utils.copy(blob, toBeWrittenInWorkingDir);
        }

    }

    /**
     * a.
     * @param workingDir a
     * @param commitID a
     * @param fileName a
     */
    static void checkoutFileFromCommitToWorkingDir(String workingDir,
                                                   String commitID,
                                                   String fileName) {
        File repo = new File(workingDir, ".gitlet");
        String fullCommitTag = getCommitString(workingDir, commitID);
        Commit commitFromCheckout = Commit.getAnyCommit(workingDir,
                fullCommitTag);
        String sha1StringFromCommitForFileName
                = commitFromCheckout.getBlobs().get(fileName);
        if (sha1StringFromCommitForFileName == null) {
            throw new GitletException("File does not exist in that commit.");
        } else {
            File workingFileToEdit = new File(workingDir, fileName);
            File blobFileRepo = new File(repo.getAbsolutePath(),
                    sha1StringFromCommitForFileName);
            Utils.copy(blobFileRepo, workingFileToEdit);
        }
    }

    /**
     * yup.
     *
     * @param workingDir a
     * @param branchName a
     */
    static void checkoutBranch(String workingDir, String branchName) {
        File repo = new File(workingDir, ".gitlet");
        File commitDir = new File(repo.getAbsolutePath(), ".commit");
        File getBranchFile = new File(repo.getAbsolutePath(),
                branchName + ".branch");
        if (!getBranchFile.exists()) {
            throw new GitletException("No such branch exists.");
        }
        Branch checkoutBranch = new Branch(getBranchFile);
        Branch currentBranch = Branch.getCurrentBranch(workingDir);
        if (checkoutBranch.getName().equals(currentBranch.getName())) {
            throw new GitletException("No need "
                    + "to checkout the current branch.");
        }
        Commit checkoutBranchHead = Commit.getHeadCommit(workingDir,
                checkoutBranch);
        Commit currentBranchHead = Commit.getHeadCommit(workingDir,
                currentBranch);
        checkoutToCommit(workingDir,
                currentBranchHead,
                checkoutBranchHead);
        File headToChange = new File(repo.getAbsolutePath(),
                "HEAD");
        Utils.writeContents(headToChange, branchName);
    }

    /**
     * yup.
     *
     * @param workingDir a
     * @param fileName   a
     */
    static void remove(String workingDir, String fileName) {
        ChangeList additions = ChangeList.readFromRepo(workingDir,
                "add");
        ChangeList removals = ChangeList.readFromRepo(workingDir,
                "remove");
        File repo = new File(workingDir, ".gitlet");
        Branch currentBranch = Branch.getCurrentBranch(workingDir);
        Commit currentCommit
                = Commit.getHeadCommit(workingDir, currentBranch);
        File workingFile = new File(workingDir, fileName);
        boolean wasStagedOrTracked = false;
        if (additions.containsKey(fileName)) {
            additions.removeFile(workingDir, fileName);
            wasStagedOrTracked = true;
        }
        if (currentCommit.getBlobs().containsKey(fileName)) {
            wasStagedOrTracked = true;
            removals.put(fileName, "");
            removals.writeToRepo(workingDir);
            if (workingFile.exists()) {
                workingFile.delete();
            }
        }
        if (!wasStagedOrTracked) {
            throw new GitletException(" No reason to remove the file.");
        }
    }

    /**
     * yup.
     *
     * @param workingDir a
     */
    static void log(String workingDir) {
        File repo = new File(workingDir, ".gitlet");
        File commitDir = new File(repo.getAbsolutePath(), ".commit");
        Branch headBranch = Branch.getCurrentBranch(workingDir);
        String headCommit = headBranch.getHead();
        File latestCommitFile = new File(commitDir, headCommit);
        Commit latestCommit = new Commit(latestCommitFile);
        String printName = headCommit;
        while (true) {
            printLog(latestCommit, printName);
            String latestCommitParent = latestCommit.getParentCommit();
            if (latestCommitParent == null) {
                break;
            } else {
                System.out.println("");
                latestCommitFile = new File(commitDir.getAbsolutePath(),
                        latestCommitParent);
                latestCommit = new Commit(latestCommitFile);
                printName = latestCommitParent;
            }
        }
    }

    /**
     * yup.
     *
     * @param commitToPrintLog a
     * @param commitName       a
     */
    static void printLog(Commit commitToPrintLog, String commitName) {
        Date commitDate = commitToPrintLog.returnDate();
        String commitMessage = commitToPrintLog.getMessage();
        System.out.println("===");
        System.out.println("commit " + commitName);
        if (commitToPrintLog.getSecondParent() != null) {
            System.out.println("Merge: "
                    + commitToPrintLog.getParentCommit().substring(0, 7)
                    + " " + commitToPrintLog.getSecondParent().substring(0, 7));
        }
        SimpleDateFormat myFormat =
                new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
        StringBuffer newDate = new StringBuffer();
        myFormat.format(commitDate, newDate, new FieldPosition(0));
        String finalDate = newDate.toString();
        System.out.println("Date: " + finalDate);
        System.out.println(commitMessage);
    }

    /**
     * yup.
     *
     * @param workingDir a
     * @param message    a
     */
    static void findFromMessage(String workingDir, String message) {
        File repo = new File(workingDir, ".gitlet");
        File commitDir = new File(repo.getAbsolutePath(), ".commit");
        List<String> commitHashes = Utils.plainFilenamesIn(commitDir);
        boolean found = false;
        for (String commitHash : commitHashes) {
            File commitFileFromHash
                    = new File(commitDir.getAbsolutePath(), commitHash);
            Commit actualCommit = new Commit(commitFileFromHash);
            if (actualCommit.getMessage().equals(message)) {
                System.out.println(commitHash);
                found = true;
            }
        }
        if (!found) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    /**
     * yup.
     *
     * @param workingDir    a
     * @param newBranchName a
     */
    static void makeNewBranch(String workingDir, String newBranchName) {
        File repo = new File(workingDir, ".gitlet");
        File branchFileWithName
                = new File(repo.getAbsolutePath(),
                newBranchName + ".branch");
        if (branchFileWithName.exists()) {
            throw new GitletException("A branch"
                    + " with that name already exists.");
        } else {
            Branch newBranch = Branch.getCurrentBranch(workingDir);
            newBranch.setName(newBranchName);
            newBranch.writeToRepo(workingDir);
        }
    }

    /**
     * yup.
     *
     * @param workingDir    a
     * @param startOfCommit a
     * @return a
     */
    static String getCommitString(String workingDir, String startOfCommit) {
        File repo = new File(workingDir, ".gitlet");
        File commitDir = new File(repo.getAbsolutePath(), ".commit");
        List<String> fileNamesInRepo = Utils.plainFilenamesIn(commitDir);
        int lengthOfGiven = startOfCommit.length();
        for (String fileName : fileNamesInRepo) {
            if (fileName.length() >= startOfCommit.length()
                    && fileName.substring(0,
                    lengthOfGiven).equals(startOfCommit)) {
                return fileName;
            }
        }
        throw new GitletException("No commit with that id exists.");
    }

    /**
     * yup.
     *
     * @param workingDir a
     */
    static void globalLog(String workingDir) {
        File repo = new File(workingDir, ".gitlet");
        File commitDir = new File(repo.getAbsolutePath(),
                ".commit");
        List<String> fileCommitNames = Utils.plainFilenamesIn(commitDir);
        for (String commitHash : fileCommitNames) {
            File commitFile = new File(commitDir.getAbsolutePath(),
                    commitHash);
            Commit commitToPrintLog = new Commit(commitFile);
            printLog(commitToPrintLog, commitHash);
        }
    }

    /**
     * yup.
     *
     * @param workingDir a
     */
    static void status(String workingDir) {
        File repo = new File(workingDir, ".gitlet");
        String headBranchString = Branch.getCurrentBranch(workingDir).getName();
        File[] files = repo.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".branch");
            }
        });
        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileNames[i] = files[i].getName().substring(0,
                    files[i].getName().lastIndexOf('.'));
        }
        Arrays.sort(fileNames);
        System.out.println("=== Branches ===");
        for (String branchName : fileNames) {
            if (branchName.equals(headBranchString)) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println("");
        System.out.println("=== Staged Files ===");
        ChangeList add = ChangeList.readFromRepo(workingDir,
                "add");
        if (add.size() > 0) {
            Set<String> fileNamesAdded = add.keySet();
            String[] stringArrayOfFileNames = Arrays
                    .copyOf(
                            fileNamesAdded.toArray(),
                            fileNamesAdded.size(),
                            String[].class);
            Arrays.sort(stringArrayOfFileNames);
            for (String fileName : stringArrayOfFileNames) {
                System.out.println(fileName);
            }
        }
        System.out.println("");
        System.out.println("=== Removed Files ===");
        ChangeList remove = ChangeList.readFromRepo(workingDir,
                "remove");
        if (remove.size() > 0) {
            Set<String> fileNamesToRemove = remove.keySet();
            String[] stringArrayOfFileNames = Arrays
                    .copyOf(
                            fileNamesToRemove.toArray(),
                            fileNamesToRemove.size(),
                            String[].class);
            Arrays.sort(stringArrayOfFileNames);
            for (String fileName : stringArrayOfFileNames) {
                System.out.println(fileName);
            }
        }
        System.out.println("");
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println("");
        System.out.println("=== Untracked Files ===");
    }

    /**
     * yup.
     *
     * @param workingDir a
     * @param branchName a
     */
    static void removeBranch(String workingDir, String branchName) {
        File repo = new File(workingDir, ".gitlet");
        if (Branch.getCurrentBranch(workingDir).getName().equals(branchName)) {
            throw new GitletException("Cannot remove the current branch.");
        } else {
            File branchFile = new File(repo, branchName + ".branch");
            if (!branchFile.exists()) {
                throw new GitletException("A branch "
                        + "with that name does not exist.");
            } else {
                branchFile.delete();
            }
        }
    }

    /**
     * yup.
     *
     * @param workingDir      a
     * @param shortenedCommit a
     */
    static void resetToCommit(String workingDir, String shortenedCommit) {
        String fullCommitString = getCommitString(workingDir,
                shortenedCommit);
        Commit commitToResetTo = Commit.getAnyCommit(workingDir,
                fullCommitString);
        Branch currentBranch = Branch.getCurrentBranch(workingDir);
        Commit currentCommit = Commit.getHeadCommit(workingDir,
                currentBranch);
        checkoutToCommit(workingDir, currentCommit,
                commitToResetTo);
        currentBranch.setHead(commitToResetTo.getHash());
    }

    /**
     * yup.
     *
     * @param workingDir     a
     * @param currentCommit  a
     * @param checkoutCommit a
     */
    static void checkoutToCommit(String workingDir,
                                 Commit currentCommit,
                                 Commit checkoutCommit) {
        File repo = new File(workingDir, ".gitlet");
        Set<String> trackedFiles = currentCommit.getBlobs().keySet();
        Set<String> filesInCommitOfCheckoutHead
                = checkoutCommit.getBlobs().keySet();
        for (String fileToBeChanged : filesInCommitOfCheckoutHead) {
            File workingDirFile
                    = new File(workingDir, fileToBeChanged);
            if (!trackedFiles.contains(fileToBeChanged)
                    && workingDirFile.exists()) {
                throw new GitletException("There "
                        + "is an untracked file in the way; "
                        + "delete it or add it first.");
            }
        }
        for (String fileNamesToBeChangeInWorkingDir
                : filesInCommitOfCheckoutHead) {
            String blobIDFromCheckout
                    = checkoutCommit.getBlobs()
                    .get(fileNamesToBeChangeInWorkingDir);
            File blobFile
                    = new File(repo.getAbsolutePath(),
                    blobIDFromCheckout);
            File targetFile = new File(workingDir,
                    fileNamesToBeChangeInWorkingDir);
            Utils.copy(blobFile, targetFile);
        }
        for (String fileNameInCurrentCommit
                : currentCommit.getBlobs().keySet()) {
            if (!filesInCommitOfCheckoutHead
                    .contains(fileNameInCurrentCommit)) {
                File toDelete = new File(workingDir,
                        fileNameInCurrentCommit);
                toDelete.delete();
            }
        }
        ChangeList add = ChangeList.readFromRepo(workingDir,
                "add");
        add.clear();
        add.writeToRepo(workingDir);
        ChangeList rem = ChangeList.readFromRepo(workingDir,
                "remove");
        rem.clear();
        rem.writeToRepo(workingDir);
    }

    /**
     * a.
     *
     * @param workingDir a
     * @param branchName a
     * @param isDryRun   a
     * @return a
     */
    static Commit[] submergeHelper(String workingDir, String branchName,
                                   boolean isDryRun) {
        File repo = new File(workingDir, ".gitlet");
        Boolean encounteredConflict = false;
        File mergeBranchFile = new File(repo.getAbsolutePath(),
                branchName + ".branch");
        if (isDryRun) {
            if (isDirty(workingDir)) {
                throw new GitletException("You have uncommitted changes.");
            }
            if (!mergeBranchFile.exists()) {
                throw new GitletException("A branch"
                        + " with that name does not exist.");
            }
        }
        Branch mergeBranch = new Branch(mergeBranchFile);
        Branch currentBranch = Branch.getCurrentBranch(workingDir);
        if (mergeBranch.getName().equals(currentBranch.getName()) && isDryRun) {
            throw new GitletException("Cannot merge a branch with itself.");
        }

        Commit currentHead = Commit.getHeadCommit(workingDir,
                currentBranch);
        Commit mergeHead = Commit.getHeadCommit(workingDir,
                mergeBranch);
        Commit splitPoint = getSplitPoint(workingDir,
                currentBranch,
                mergeBranch);
        if (splitPoint.equals(mergeHead)) {
            System.out.println("Given branch"
                    + " is an ancestor of the current branch.");
        } else if (splitPoint.equals(currentHead)) {
            currentBranch.setHead(mergeHead.getHash());
            currentBranch.writeToRepo(workingDir);
            System.out.println("Current branch fast-forwarded.");
        }
        Commit[] commits = new Commit[3];
        commits[0] = currentHead;
        commits[1] = mergeHead;
        commits[2] = splitPoint;
        return commits;
    }

    /**
     * a.
     *
     * @param workingDir a
     * @param branchName a
     * @param isDryRun   a
     * @return a
     */
    static Branch[] submergeHelperII(String workingDir, String branchName,
                                     boolean isDryRun) {
        File repo = new File(workingDir, ".gitlet");
        Boolean encounteredConflict = false;
        File mergeBranchFile = new File(repo.getAbsolutePath(),
                branchName + ".branch");
        if (!isDryRun) {
            if (isDirty(workingDir)) {
                throw new GitletException("You have uncommitted changes.");
            }
            if (!mergeBranchFile.exists()) {
                throw new GitletException("A branch"
                        + " with that name does not exist.");
            }
        }
        Branch mergeBranch = new Branch(mergeBranchFile);
        Branch currentBranch = Branch.getCurrentBranch(workingDir);
        Branch[] branches = new Branch[2];
        branches[0] = currentBranch;
        branches[1] = mergeBranch;
        return branches;
    }

    /**
     * a.
     * @param workingDir a
     * @param branchName a
     * @param isDryRun a
     * @return a
     */
    static boolean submergeHelperIII(String workingDir,
                                     String branchName,
                                     boolean isDryRun) {
        File repo = new File(workingDir, ".gitlet");
        Boolean encounteredConflict = false;
        Commit[] commits = submergeHelper(workingDir, branchName, isDryRun);
        Commit currentHead = commits[0];
        Commit mergeHead = commits[1];
        Commit splitPoint = commits[2];
        Branch[] branches = submergeHelperII(workingDir, branchName, isDryRun);
        Branch currentBranch = branches[0];
        Branch mergeBranch = branches[1];
        for (String fileName : mergeHead.getBlobs().keySet()) {
            if (isModified(mergeHead, splitPoint, fileName)) {
                if (!isModified(currentHead, splitPoint, fileName)) {
                    File workingDirFile = new File(workingDir, fileName);
                    if (!currentHead.getBlobs().containsKey(fileName)
                            && workingDirFile.exists()) {
                        throw new GitletException("There "
                                + "is an untracked file in the"
                                + " way; delete it or add it first.");
                    }
                    if (!isDryRun) {
                        checkoutFileFromCommitToWorkingDir(workingDir,
                                mergeHead.getHash(), fileName);
                        add(workingDir, fileName);
                    }
                } else if (isModified(mergeHead, currentHead, fileName)) {
                    File workingFile = new File(workingDir, fileName);
                    String mergeContents = handleMergeConflict(workingDir,
                            currentBranch, mergeBranch, fileName);
                    if (!isDryRun) {
                        Utils.writeContents(workingFile, mergeContents);
                        add(workingDir, fileName);
                        encounteredConflict = true;
                    }
                }
            }
        }
        return encounteredConflict;
    }

    /**
     * yup.
     *
     * @param workingDir a
     * @param branchName a
     * @param isDryRun   a
     */
    static void submerge(String workingDir, String branchName,
                         boolean isDryRun) {
        File repo = new File(workingDir, ".gitlet");
        Boolean encounteredConflict = false;
        Commit[] commits = submergeHelper(workingDir,
                branchName, isDryRun);
        Commit currentHead = commits[0];
        Commit mergeHead = commits[1];
        Commit splitPoint = commits[2];
        Branch[] branches = submergeHelperII(workingDir,
                branchName, isDryRun);
        Branch currentBranch = branches[0];
        Branch mergeBranch = branches[1];
        encounteredConflict = submergeHelperIII(workingDir,
                branchName, isDryRun);
        for (String fileName : currentHead.getBlobs().keySet()) {
            if (!isModified(currentHead, splitPoint, fileName)) {
                if (!mergeHead.getBlobs().containsKey(fileName)) {
                    if (!isDryRun) {
                        remove(workingDir, fileName);
                    }
                }

            } else if (!mergeHead.getBlobs().containsKey(fileName)
                    && splitPoint.getBlobs().containsKey(fileName)) {
                File workingFile = new File(workingDir, fileName);
                String mergeContents = handleMergeConflict(workingDir,
                        currentBranch, mergeBranch, fileName);
                if (!isDryRun) {
                    Utils.writeContents(workingFile, mergeContents);
                    add(workingDir, fileName);
                    encounteredConflict = true;
                }
            }
        }
        if (!isDryRun) {
            Commit newCommit = new Commit("Merged "
                    + mergeBranch.getName() + " into "
                    + currentBranch.getName() + ".",
                    currentHead.getHash(),
                    mergeHead.getHash(), repo.getAbsolutePath());
            newCommit.applyAdditions(workingDir);
            newCommit.applyRemovals(workingDir);
            String newHead = newCommit.writeToRepo(workingDir);
            currentBranch.setHead(newHead);
            currentBranch.writeToRepo(workingDir);
            newCommit.writeToRepo(workingDir);
            if (encounteredConflict) {
                System.out.println("Encountered a merge conflict.");
            }
        }
    }

    /**
     * yup.
     *
     * @param workingDir a
     * @param branchName a
     */
    static void merge(String workingDir, String branchName) {
        submerge(workingDir, branchName, true);
        submerge(workingDir, branchName, false);
    }

    /**
     * yup.
     *
     * @param c1       a
     * @param c2       a
     * @param fileName a
     * @return a
     */
    static boolean isModified(Commit c1, Commit c2, String fileName) {
        if (!c1.getBlobs().containsKey(fileName)
                && !c2.getBlobs().containsKey(fileName)) {
            return false;
        } else if (!c1.getBlobs().containsKey(fileName)
                || !c2.getBlobs().containsKey(fileName)) {
            return true;
        } else {
            return !c1.getBlobs().get(fileName)
                    .equals(c2.getBlobs().get(fileName));
        }
    }

    /**
     * a.
     *
     * @param workingDir a
     * @param c2         a
     * @param fileName   a
     * @return a
     */
    static String getFileContents(String workingDir,
                                  Commit c2,
                                  String fileName) {
        File repo = new File(workingDir, ".gitlet");
        if (c2.getBlobs().containsKey(fileName)) {
            String blobName = c2.getBlobs().get(fileName);
            File blobFile = new File(repo.getAbsolutePath(), blobName);
            return Utils.readContentsAsString(blobFile);
        } else {
            return "";
        }
    }

    /**
     * a.
     *
     * @param workingDir a
     * @param b1         a
     * @param b2         a
     * @param fileName   a
     * @return a
     */
    static String handleMergeConflict(String workingDir,
                                      Branch b1,
                                      Branch b2,
                                      String fileName) {
        /**
         * "<<<<<<< HEAD\n"
         * contents of file 1
         * "=======\n"
         * contents of file 2
         * ">>>>>>>\n"
         */
        File repo = new File(workingDir, ".gitlet");
        Commit currentHead = Commit.getHeadCommit(workingDir, b1);
        Commit mergeHead = Commit.getHeadCommit(workingDir, b2);
        String mergeContents = "<<<<<<< HEAD\n";
        mergeContents = mergeContents
                .concat(getFileContents(workingDir,
                        currentHead, fileName))
                .concat("=======\n")
                .concat(getFileContents(workingDir,
                        mergeHead, fileName)).concat(">>>>>>>\n");
        return mergeContents;
    }

    /**
     * yup.
     *
     * @param workingDir    a
     * @param currentBranch a
     * @param mergeBranch   a
     * @return a
     */
    static Commit getSplitPoint(String workingDir,
                                Branch currentBranch,
                                Branch mergeBranch) {
        File repo = new File(workingDir, ".gitlet");
        File commitDir = new File(repo.getAbsolutePath(), ".commit");
        LinkedList<Commit> commitQueue = new LinkedList<>();
        HashSet<String> discoveredHashes = new HashSet<>();
        Commit currentBranchCommit
                = Commit.getHeadCommit(workingDir, currentBranch);
        Commit mergeBranchCommit
                = Commit.getHeadCommit(workingDir, mergeBranch);
        commitQueue.add(mergeBranchCommit);
        while (!commitQueue.isEmpty()) {
            Commit currentCommit = commitQueue.remove();
            if (!discoveredHashes.contains(currentCommit.getHash())) {
                discoveredHashes.add(currentCommit.getHash());
                if (currentCommit.getParentCommit() != null) {
                    File parentCommitFile
                            = new File(commitDir.getAbsolutePath(),
                            currentCommit.getParentCommit());
                    Commit parentCommit = new Commit(parentCommitFile);
                    commitQueue.add(parentCommit);
                    if (currentCommit.getSecondParent() != null) {
                        File secondParentCommit
                                = new File(commitDir.getAbsolutePath(),
                                currentCommit.getParentCommit());
                        Commit secondParent = new Commit(secondParentCommit);
                        commitQueue.add(secondParent);
                    }
                }
            }
        }
        commitQueue.add(currentBranchCommit);
        while (!commitQueue.isEmpty()) {
            Commit currentCommit = commitQueue.remove();
            if (discoveredHashes.contains(currentCommit.getHash())) {
                return currentCommit;
            } else {
                if (currentCommit.getParentCommit() != null) {
                    File parentCommitFile
                            = new File(commitDir.getAbsolutePath(),
                            currentCommit.getParentCommit());
                    Commit parentCommit = new Commit(parentCommitFile);
                    commitQueue.add(parentCommit);
                    if (currentCommit.getSecondParent() != null) {
                        File secondParentCommit
                                = new File(commitDir.getAbsolutePath(),
                                currentCommit.getSecondParent());
                        Commit secondParent = new Commit(secondParentCommit);
                        commitQueue.add(secondParent);
                    }
                }
            }
        }
        throw new GitletException("Traversed "
                + "both branches and didn't find a split point");
    }

    /**
     * a.
     *
     * @param workingDir a
     * @return a
     */
    static boolean isDirty(String workingDir) {
        ChangeList add = ChangeList.readFromRepo(workingDir,
                "add");
        ChangeList removal = ChangeList.readFromRepo(workingDir,
                "remove");
        return !(add.isEmpty() && removal.isEmpty());
    }
}
