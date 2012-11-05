package ca.ualberta.cs.c301_repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ca.ualberta.cs.c301_crowdclient.CrowdClient;
import ca.ualberta.cs.c301_crowdclient.CrowdSourcerEntry;
import ca.ualberta.cs.c301_interfaces.Task;

/**
 * TfTaskRepository provides a facade/interface for accessing CrowdClient and
 * the Crowd Sourcer webservice. It handles adding, updating, and getting of
 * tasks from CrowdClient through static methods. It deals with the input and 
 * output expected by the application (namely, task objects).  
 * @author colinhunt
 *
 */
public class TfTaskRepository {
    
    private static CrowdClient crowdClient = new CrowdClient();
    
    // TODO Can we keep this list up-to-date and then save internally onPause?
    // Then just check if can connect to server, if not, return this list
    private List<Task> taskList = new ArrayList<Task>();

    /**
     * Adds a task to the webservice or updates an existing one. Updating is
     * dependent on the task id already existing or not.
     * @param task object to add/update
     */
    public static void addTask(Task task) {
        try {
            CrowdSourcerEntry entry = new CrowdSourcerEntry();
            // We set the device id into the summary
            entry.setSummary(task.getTitle());
            entry.setDescription(task.getDeviceId());
            entry.setContent((TfTask) task);

            String taskId = task.getTaskId();
            if (taskId.isEmpty()) {
                crowdClient.insertEntry(entry);                
            } else {
                entry.setId(taskId);
                crowdClient.updateEntry(entry);
            }
        } catch (Exception e) {
            System.err.println("<<<Error adding the task>>>");
            e.printStackTrace(System.err);
        }
    }
    
    /**
     * Gets a list of tasks that match a given device id from the webservice.
     * @param deviceId
     * @return A list of tasks.
     * @throws Exception
     */
    public static List<Task> getTasksByDeviceId(String deviceId) throws Exception {
        List<Task> taskList = getAllTasks();
        for (Task task : taskList) {
            if (task.getDeviceId() == deviceId) {
                taskList.add(task);
            }
        }
        return taskList;
    }

    /**
     * Gets all tasks that are currently in the webservice.
     * @return A list of tasks.
     * @throws Exception
     */
    public static List<Task> getAllTasks() throws Exception {
        List<CrowdSourcerEntry> entryList = crowdClient.getEntryList();
        List<Task> taskList = new ArrayList<Task>();
        for (CrowdSourcerEntry entry : entryList) {
            Task task = entry.getContent();
            task.setTaskId(entry.getId());
            taskList.add(task);
        }
        return taskList;
    }
    
    /**
     * Gets the list of entries as maps in the webservice.
     * Basic, fast representation of the tasks currently stored. No content is
     * returned.
     * @return A list of maps.
     * @throws Exception
     */
    public static List<Map<String,String>> getShallowEntries() 
            throws Exception {
        return crowdClient.getShallowList();
    }

    /**
     * Gets a single task associated with a given a task id.
     * @param taskId of the task to be returned.
     * @return A task.
     * @throws Exception
     */
    public static Task getTaskById(String taskId) throws Exception {
        CrowdSourcerEntry entry = crowdClient.getEntry(taskId);
        Task task = entry.getContent();
        if (task == null) {
            throw new Exception("TfTaskRepository got a null task in " +
            		"getTaskById() with id=" + taskId);
        }
        return task;
    }

}