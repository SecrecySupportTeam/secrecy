package com.doplgangr.secrecy;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.ipaulpro.afilechooser.FileChooserActivity;

import java.util.ArrayList;


public class ChooseFolder extends ActionBarActivity {
    private static final int REQUEST_CODE = 6384; // onActivityResult request code
    private static final ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<String>();
    static {
        INCLUDE_EXTENSIONS_LIST.add(".");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, FileChooserActivity.class);

        intent.putStringArrayListExtra(
                FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                INCLUDE_EXTENSIONS_LIST);
        intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, true);
        startActivityForResult(intent, REQUEST_CODE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.choose_folder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
