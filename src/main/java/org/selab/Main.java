package org.selab;

import db.DBManager;
import file.FileIOManager;

import org.selab.jcodelib.diffutil.DiffResult;
import org.selab.jcodelib.diffutil.TreeDiff;
import org.selab.jcodelib.element.IJMChange;
import org.selab.jcodelib.jgit.ReposHandler;
import kr.ac.seoultech.selab.esscore.model.ESNodeEdit;
import kr.ac.seoultech.selab.esscore.model.Script;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    //Hard-coded projects - need to read it from DB.
    public static String baseDir = "subjects";
    public static final String oldDir = "old";
    public static final String newDir = "new";
    public static long commitCountOfIJM = 1;
    public static String [] projectArr = {"elasticsearch", "ballerina-lang", "crate", "neo4j","sakai", "wildfly"};

    public static void main(String[] args) throws IOException {

        // Put SCD Tool here.
        String toolIJM = "IJM";
        DBManager db = null;

        // use command line arguments
        String project = args[0];

        try {
            //Change db.properties.
            db = new DBManager("src/main/resources/db.properties");

            //Connect DB.
            Connection con = db.getConnection();

            // if exception occurs, do rollback
            con.setAutoCommit(false);

            // Collect and store IJM EditOp, runtime in DB.
            PreparedStatement psIJM = con.prepareStatement("insert into changes_IJM " +
                    " ( file_id, tool, change_type, entity_type, old_start_pos, " +
                    " old_length, new_start_pos, new_length ) " +
                    " values ( ?, ?, ?, ?, ?, " +
                    "?, ?, ? )");
            PreparedStatement psIJMrunTime = con.prepareStatement("insert into changes_IJM_runtime " +
                    " (file_id, runtime_gitReset, runtime_editScript )" +
                    " values(?, ?, ?)");

            System.out.println("Collecting Changes from " + project);
            String oldReposPath = String.join(File.separator, baseDir, oldDir, project) + File.separator;
            String newReposPath = String.join(File.separator, baseDir, newDir, project) + File.separator;
            File oldReposDir = new File(oldReposPath);
            File newReposDir = new File(newReposPath);

            // Prepare files.
            List<String> fileInfo = new ArrayList<>();

            PreparedStatement fileSel = con.prepareStatement(
                    " select c.commit_id commit_id, c.old_commit old_commit, c.new_commit new_commit, " +
                            " f.file_path file_path, f.file_id file_id " +
                            " from commits c, files f where c.commit_id = f.commit_id and c.project_name = '" + project + "'" +
                            " and c.merged_commit_status != 'T' " +
                            " order by file_id, commit_id ");
            ResultSet fileRS = fileSel.executeQuery();
            while (fileRS.next()) {
                StringBuilder sb = new StringBuilder();
                sb.append(fileRS.getInt("commit_id"))
                        .append(",")
                        .append(fileRS.getString("old_commit"))
                        .append(",")
                        .append(fileRS.getString("new_commit"))
                        .append(",")
                        .append(fileRS.getString("file_path"))
                        .append(",")
                        .append(fileRS.getInt("file_id"));
                fileInfo.add(sb.toString());
                sb.setLength(0);
            }

            fileRS.close();
            fileSel.close();

            System.out.println("Total " + fileInfo.size() + " revisions.");

            for (int i = 0; i < fileInfo.size(); i++) {
                String key = fileInfo.get(i);
                String[] tokens = key.split(",");
                String commitId = tokens[0];
                String oldCommitId = tokens[1];
                String newCommitId = tokens[2];
                String filePath = tokens[3];
                String fileId = tokens[4];
                System.out.println("CommitId : " + commitId + ", fileId : " + fileId + ", oldCommitId : " + oldCommitId + ", newCommitId : " + newCommitId);

                // Reset hard to old/new commit IDs.
                long gitResetStartTime = System.currentTimeMillis();
                ReposHandler.update(oldReposDir, oldCommitId);
                ReposHandler.update(newReposDir, newCommitId);
                long gitResetFinishTime = System.currentTimeMillis();
                long gitResetElapsedTime = gitResetFinishTime - gitResetStartTime;

                try {
                    if (filePath.contains("/test/")) // ignore test code in GitHub project
                        continue;

                    List<String> projectList = new ArrayList<>(Arrays.asList(projectArr));
                    if (!filePath.contains("/org/") && projectList.contains(project))
                        continue;

                    File oldFile = new File(oldReposPath + filePath);
                    File newFile = new File(newReposPath + filePath);
                    String oldCode = "";
                    String newCode = "";

                    // Handle FileNotFoundException in oldFile, newFile
                    oldCode = FileIOManager.getContent(oldFile).intern();
                    newCode = FileIOManager.getContent(newFile).intern();

                    //Practically these files are deleted/inserted.
                    if (oldCode.length() == 0 || newCode.length() == 0) {
                        continue;
                    }

                    // Apply Source Code Differencing Tools: IJM
                    DiffResult diffResultOfIJM = null;
                    try {
                        diffResultOfIJM = TreeDiff.diffIJM(oldFile, newFile);
                    } catch (NullPointerException e) {
                        System.out.println("e = " + e);
                    } finally {
                        if (diffResultOfIJM != null) {
                            long runtimeOfIJM = diffResultOfIJM.getRuntime();

                            List<IJMChange> scriptIJMList = (List<IJMChange>) diffResultOfIJM.getScript();
                            for (IJMChange ijmChange : scriptIJMList) {
                                psIJM.clearParameters();
                                psIJMrunTime.clearParameters();

                                psIJM.setInt(1, Integer.parseInt(fileId));         //file_id
                                psIJM.setString(2, toolIJM);          
                                psIJM.setString(3, ijmChange.getChangeType());   // change_type
                                psIJM.setString(4, ijmChange.getEntityType());
                                psIJM.setInt(5, ijmChange.getOldStartPos());
                                psIJM.setInt(6, ijmChange.getOldLength());
                                psIJM.setInt(7, ijmChange.getNewStartPos());
                                psIJM.setInt(8, ijmChange.getNewLength());

                                psIJMrunTime.setInt(1, Integer.parseInt(fileId));         //file_id
                                psIJMrunTime.setLong(2, gitResetElapsedTime);
                                psIJMrunTime.setLong(3, runtimeOfIJM);

                                psIJM.addBatch();
                                psIJMrunTime.addBatch();
                                commitCountOfIJM++;
                            }
                        }
                    }
                    if (commitCountOfIJM % 100 == 0) {
                        psIJM.executeBatch();
                        psIJMrunTime.executeBatch();
                        con.commit();
                        psIJM.clearBatch();
                        psIJMrunTime.clearBatch();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        // If failed, do rollback
                        con.rollback();
                    } catch (SQLException E) {
                        E.printStackTrace();
                    }
                }
                // Committing for the rest of the syntax that has not been committed
                psIJM.executeBatch();
                psIJMrunTime.executeBatch();
                con.commit();
                psIJM.clearBatch();
                psIJMrunTime.clearBatch();
            }


            if (psIJM != null){psIJM.close(); psIJM = null;}
            if (psIJMrunTime != null){psIJMrunTime.close(); psIJMrunTime = null;}
            if (con != null){con.close(); con = null;}
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }
}
