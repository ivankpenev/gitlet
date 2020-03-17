/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author ivanp
 */
package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;


/**
 * @author ivanp
 */
public class Commit implements Serializable {
    /**
     * Driver class for Gitlet, the tiny stupid version-control system.
     *
     * @author ivanp
     */
    /**
     * a.
     */
    private String _parentCommitKey;

    /**
     * a.
     */
    private String _message;
    /**
     * done.
     */
    private Date _commitDate;
    /**
     * done.
     */
    private HashMap<String, String> _blobs;
    /**
     * done.
     */
    private String _secondParent;

    /**
     * a.
     *
     * @param message         a
     * @param parentCommitKey a
     * @param repoDir         a
     */
    Commit(String message,
           String parentCommitKey, String repoDir) {
        _message = message;
        _commitDate = new Date(0);
        _parentCommitKey = parentCommitKey;
        _secondParent = null;
        if (parentCommitKey == null) {
            _blobs = new HashMap<String, String>();
        } else {
            File commitDir = new File(repoDir, ".commit");
            File parentCommitFile = new File(commitDir, parentCommitKey);
            Commit parentCommit = new Commit(parentCommitFile);
            _blobs = parentCommit._blobs;
        }
    }

    /**
     * a.
     *
     * @param message               a
     * @param parentCommitKey       a
     * @param secondParentCommitKey a
     * @param repoDir               a
     */
    Commit(String message,
           String parentCommitKey,
           String secondParentCommitKey,
           String repoDir) {
        this(message, parentCommitKey, repoDir);
        _secondParent = secondParentCommitKey;
    }

    /**
     * a.
     *
     * @param f a
     */
    Commit(File f) {
        Commit result
                = Utils.readObject(f, Commit.class);
        _parentCommitKey = result._parentCommitKey;
        _message = result._message;
        _commitDate = result._commitDate;
        _blobs = result._blobs;
        _secondParent = result._secondParent;
    }

    /**
     * a.
     *
     * @param parentDir a
     * @return a
     */
    public String writeToRepo(String parentDir) {
        File repo = new File(parentDir,
                ".gitlet");
        File commitDir = new File(repo.getAbsolutePath(),
                ".commit");
        File commitFile = Utils.join(commitDir,
                this.getHash());
        Utils.writeObject(commitFile, this);
        return this.getHash();
    }

    /**
     * a.
     *
     * @param parentDir        a
     * @param hashNameOfCommit a
     * @return a
     */
    public static Commit getAnyCommit(String parentDir,
                                      String hashNameOfCommit) {
        File repo = new File(parentDir,
                ".gitlet");
        File commitDir = new File(repo.getAbsolutePath(),
                ".commit");
        File commitToReturn = new File(commitDir,
                hashNameOfCommit);
        return new Commit(commitToReturn);
    }

    /**
     * a.
     *
     * @return a
     */
    public String getParentCommit() {
        return _parentCommitKey;
    }

    /**
     * a.
     *
     * @return a
     */
    public String getHash() {
        return Utils.sha1(Utils.serialize(this));
    }

    /**
     * a.
     *
     * @param workingDir    a
     * @param workingBranch a
     * @return a
     */
    public static Commit getHeadCommit(String workingDir,
                                       Branch workingBranch) {
        File repo = new File(workingDir, ".gitlet");
        File commitDir = new File(repo, ".commit");
        String branchHeadCommit = workingBranch.getHead();
        File headCommitFile = new File(commitDir.getAbsolutePath(),
                branchHeadCommit);
        return new Commit(headCommitFile);
    }

    /**
     * a.
     *
     * @param parentDir a
     */
    public void applyAdditions(String parentDir) {
        ChangeList additions
                = ChangeList.readFromRepo(parentDir, "add");
        for (String key : additions.keySet()) {
            _blobs.put(key, additions.get(key));
        }
        ChangeList emptyAdditions
                = new ChangeList("add");
        emptyAdditions.writeToRepo(parentDir);
    }

    /**
     * a.
     *
     * @param parentDir a
     */
    public void applyRemovals(String parentDir) {
        ChangeList removals
                = ChangeList.readFromRepo(parentDir, "remove");
        for (String key : removals.keySet()) {
            _blobs.remove(key);
        }
        ChangeList emptyRemovals
                = new ChangeList("remove");
        emptyRemovals.writeToRepo(parentDir);
    }

    /**
     * a.
     */
    public void updateNewDate() {
        _commitDate = new Date();
    }

    /**
     * a.
     *
     * @return a
     */
    public HashMap<String, String> getBlobs() {
        return _blobs;
    }

    /**
     * a.
     *
     * @param commit a
     * @return a
     */
    String getParent(Commit commit) {
        return commit._parentCommitKey;
    }


    /**
     * a.
     *
     * @return a
     */
    String getSecondParent() {
        return _secondParent;
    }

    /**
     * return date.
     *
     * @return a
     */
    Date returnDate() {
        return _commitDate;
    }

    /**
     * return message.
     *
     * @return a
     */
    String getMessage() {
        return _message;
    }

    /**
     * a.
     *
     * @param parentHash a
     */
    void assignParent(String parentHash) {
        this._parentCommitKey = parentHash;
    }

    /**
     * a.
     *
     * @param message a
     */
    void assignMessage(String message) {
        this._message = message;
    }
}
