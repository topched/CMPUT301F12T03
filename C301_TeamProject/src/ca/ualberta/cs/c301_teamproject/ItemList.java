/**
 * Task Force Application
 * See github for license and other information: 
 * github.com/tullyj/CMPUT301F12T03/
 * 
 * Task Force is created by: 
 * Colin Hunt, Edwin Chung, 
 * Kris Kushniruk, and Tully Johnson.
 */
package ca.ualberta.cs.c301_teamproject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import ca.ualberta.cs.c301_emailClient.GmailSender;
import ca.ualberta.cs.c301_interfaces.ItemType;
import ca.ualberta.cs.c301_interfaces.TaskItem;
import ca.ualberta.cs.c301_interfaces.Visibility;
import ca.ualberta.cs.c301_preview.PreviewAudio;
import ca.ualberta.cs.c301_preview.PreviewPhoto;
import ca.ualberta.cs.c301_preview.PreviewText;
import ca.ualberta.cs.c301_preview.PreviewVideo;
import ca.ualberta.cs.c301_repository.TfTaskItem;
import ca.ualberta.cs.c301_repository.TfTaskRepository;
import ca.ualberta.cs.c301_utils.Utility;

/**
 * Displays a listview of files for a given item in a task.
 * @author tullyj
 */
public class ItemList extends Activity {

	static final int DIALOG_AUDIO = 1;
	static final int DIALOG_PHOTO = 2;
	static final int DIALOG_VIDEO = 5;
	static final int TEXT_INTENT = 6;
	static final int FILE_INTENT = 7;
	private ItemType itemType;
	private TaskItem item;
	public static File currFile;
	private updateTask updateT;
	private boolean fulfilled = true;
	private int positionClicked;
	private static File savingFile = null;
	private String directory = null;
	public ArrayList<ItemListElement> listElements = 
	        new ArrayList<ItemListElement>();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_list);        

        // Get Task and Item
        String[] inArgs = getIntent().getStringArrayExtra("SendItem");
        // Set title of Type of list
        ((TextView) findViewById(R.id.listTitle)).setText(inArgs[1] + " List");
        // Set Description from item.getDescription()
        ((TextView) findViewById(R.id.listItemDesc)).setText(inArgs[2]);
        
        if (inArgs[0].equals("TEXT")) {
            itemType = ItemType.TEXT;
        } else if (inArgs[0].equals("PHOTO")) { 
            itemType = ItemType.PHOTO;
        } else if (inArgs[0].equals("VIDEO")) {
            itemType = ItemType.VIDEO;
        } else {
            itemType = ItemType.AUDIO;
        }
        
        // Set the progress bar and textview listItemFraction.
        int[] progress = populateList();
        
        double dPerc = ((double)progress[0]) / ((double)progress[1]);
        dPerc *= 100;
        int percentage = ((Double)(dPerc)).intValue();
        ((ProgressBar) findViewById(R.id.progressBar1)).setProgress(percentage);
        
        String frac = Integer.toString(progress[0]) + "/" + 
                Integer.toString(progress[1]); 
        ((TextView) findViewById(R.id.listItemFraction)).setText(frac);
        
        directory = Environment.getExternalStorageDirectory().
                getAbsolutePath() + "/TaskForce/" + 
                ViewSingleTask.task.getTitle() + "/" + 
                itemType.toString() + "/";

        
        updateT = new updateTask();
        
        Toast.makeText(ItemList.this, "Click to preview files, long click to "
                + "save the file to device.", Toast.LENGTH_LONG).show();
        
        updateList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_item_list, menu);
        return true;
    }
    
    @Override
    /**
     * When the menu button item "About" is selected display about dialog.
     * @param item	item clicked.
     */
    public boolean onOptionsItemSelected(MenuItem item){
    	Dialog helpDialog = onCreateDialog(MainPage.DIALOG_ABOUT);
        helpDialog.show();
        return true;
    }
    
    /**
     * Creates a dialog for this activity (InputFile).
     * @param id		Selects what dialog to create/return.
     * @return 			Dialog to be displayed.
     */
    public Dialog onCreateDialog(int id){   
		if (id == MainPage.DIALOG_ABOUT) {
			PromptDialog mDialog = new PromptDialog();
			return mDialog.aboutPrompt(this);
		}
		return null;
    }
    
    /**
     * Looks through the current item within a task for the list of files.
     */
    private int[] populateList(){                
        item = ViewSingleTask.task.getItemByType(itemType.toString());
    	
        // Total is the target total number for this item.
        int total = item.getNumber();
    	List<File> files = item.getAllFiles();
    	// Count will be number of files currently for a given item.
    	int count = files.size();
    	
    	for (int i = 0; i < count; i++) {
    		listElements.add(new ItemListElement(
    		        Utility.getIconFromType(itemType), itemType + " " + (i+1), 
    				getTime(files.get(i).lastModified())));
    	}
    	
    	return new int[]{count, total};
    }
    
    /**
     * Parse long format date to a given string format.
     * @param time
     * @return     String formated to date.
     */
    public String getTime(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return format.format(date).toString();
    }
    
    /**
     * Starts intent (screen) for inputing files of the given item type.
     * @param v
     */
    public void inputFileClick(View v) {
    	Intent intent = null;
    	int num = getItemNum(itemType);
    	
    	if (num != TEXT_INTENT)
	    	intent = new Intent(this, InputFile.class);
    	else
    		intent = new Intent(this, InputText.class);
    	
    	String[] strList = new String[] {
    	        String.valueOf(num), 
                itemType.toString()
        };
    	
    	intent.putExtra("ItemArgs", strList);
        startActivityForResult(intent, FILE_INTENT);
    }
    
    /**
     * Given an ItemType enum, find the corresponding const int to choose 
     * file type for the item.
     * @param type
     * @return	int of item type.
     */
    private int getItemNum(ItemType type) {
    	switch (type) {
	    	case TEXT:
				return TEXT_INTENT;
	    	case PHOTO:
				return DIALOG_PHOTO;
			case AUDIO:
				return DIALOG_AUDIO;
			case VIDEO:
				return DIALOG_VIDEO;
			default:
				return 0;
    	}
    }
    
	/**
	 * Refreshes, or recreates, the listview on the ItemList screen.
	 */
    public void updateList(){   	
    	
    	ItemListElement[] elements = new ItemListElement[listElements.size()];
        listElements.toArray(elements);
        
        ItemListAdapter adapter = new ItemListAdapter(this, R.layout.list_multi, 
                elements);
        ListView listView = (ListView) findViewById(R.id.importList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
			        int position, long id) {
				// Get the file the user selected and save the uri to file.
                Uri mUri = Uri.fromFile(item.getFile(position));
				Intent intent = new Intent(getApplicationContext(), 
				        Utility.getPreviewClass(itemType));
		    	intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
		    	// Keep a reference to the file the user selected.
		    	currFile = item.getFile(position);
				startActivity(intent);
			}
        });
        listView.setOnItemLongClickListener(new OnItemLongClickListener(){
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id){

                positionClicked = position;
                
                //here was your stuff tully. Just moved it down so there
                //was a dialog asking if they wanted to save
                saveTaskItemToDevice();               
                return true;
            }
        });
    }
    
    /**
     * This method just shows an alert dialog asking the user if they want
     * to save the item to the local device
     */
    public void saveTaskItemToDevice() {
                
        final AlertDialog.Builder saveItem = 
                new AlertDialog.Builder(ItemList.this);
        saveItem.setTitle("Save item to device?");
        
        //setting the confirm button and click listener
        saveItem.setPositiveButton("Confirm", 
                new DialogInterface.OnClickListener() {

            //we know to remove the task here
            public void onClick(DialogInterface arg0, int arg1) {
                
                savingFile = item.getFile(positionClicked);
                String fileName = savingFile.getName() + 
                        Utility.getFileExtFromType(item.getType());
                try {
                    // Create the directory, if it does not exist
                    File folder = new File(directory);
                    folder.mkdirs();
                    File nFile = new File(directory + "/" + fileName);
                    // Create the file
                    nFile.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(nFile);
                    // Create a byte array of file to save to device
                    byte[] mByte = new byte[(int) savingFile.length()];
                    DataInputStream dataIs = new DataInputStream(
                            new FileInputStream(savingFile));
                    dataIs.readFully(mByte);
                    dataIs.close();
                    // Write to the empty created file.
                    fOut.write(mByte);
                    fOut.close();
                    
                    Toast.makeText(ItemList.this, "File save locally to Path: "
                            + directory, Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                                       
                }                               
        });//end of confirm button
        
        //setting the cancel button and click listener
        saveItem.setNegativeButton("Cancel", 
                new DialogInterface.OnClickListener() {

            //cancel was clicked -- do nothiing
            public void onClick(DialogInterface dialog, int which) {
            }                                       
        });//end of cancel button
        
        //show the alert dialog
        saveItem.show();
                      
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == FILE_INTENT) {
    		if (resultCode == RESULT_OK)
		        updateT.execute();
    	}
    }
    
    /**
     * The sending of the email works. Need to build a better notification
     * email though for sure
     */
    public void sendAutoEmail(){
        String us = "taskforcenotification@gmail.com";
        String pass = "taskforcefuckyeah";
        String sub = "Task Force Notification";
        
        String to = ViewSingleTask.task.getEmail();
        String title = ViewSingleTask.task.getTitle();
        
        String body1 = "This is task force automatic notification.\n\n" + 
                "Your task: " + title + " appears to be fulfilled\n\n";
        
        String body2 = "Please visit TaskForce to view the progress.";
        
        String body = body1 + body2;
        
        try {   
            GmailSender sender = new GmailSender(us, pass);
            sender.sendMail(sub, body, us, to);   
        } catch (Exception e) {   
            Log.e("SendMail", e.getMessage(), e);   
        }       
    }
    
    /**
     * This method check is a task has been fulfilled. It is called right
     * after a task has been updated
     * @return  True iff the task is fulfilled
     */
    public boolean checkFulfillment() {
        
        //getting all the task items and setting the iterator
        List<TfTaskItem> tasks = ViewSingleTask.task.getAllItems();            
        Iterator<TfTaskItem> it = tasks.iterator();
        
        //checking if the task is fulfilled. The task is fulfilled iff
        //every items desired number is met
        while (it.hasNext()) {
            
            //grab a single item 
            TfTaskItem task = (TfTaskItem) it.next();
            
            //grab the list of files for the item
            List<File> files = task.getAllFiles();
            
            //count of each
            int desiredNum = task.getNumber();
            int actualNum = files.size();
            
            //if we have actualNum < desiredNum item not fulfilled
            //therefore task is not fulfilled
            if (actualNum<desiredNum) {
                //fulfilled = false;
                //break;
                return false;
            }            
        }
        
        return true;
    }
    
    /**
     * Displays dialog to show the file/item of task is being saved.
     */
    private class updateTask extends AsyncTask<String, String, String>{
        
        Dialog load = new Dialog(ItemList.this);

        @Override
        protected String doInBackground(String... params) {
            if (ViewSingleTask.task.isModified()) {
                try {
                    TfTaskRepository.updateTask(ViewSingleTask.task, 
                            getApplicationContext());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            //check is a task is fulfilled
            fulfilled = checkFulfillment();
            
            //grab the task visibility
            Visibility vis = ViewSingleTask.task.getVisibility();
                                         
            //fulfilled task and public - send notification email
            if(fulfilled && vis.equals(Visibility.PUBLIC))             
                sendAutoEmail();
            
            return null;
        }
        
        protected void onPreExecute(){
            //show this loading spinner
            load.setContentView(R.layout.save_load_dialog);
            load.setTitle("Saving Changes to Task");
            load.show();
        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            load.dismiss();
            finish();
        }   
    }
}
