package com.rutuldave.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Fragment for fetching the stories and displaying it as a ListView layout.
 */
public class StoriesFragment extends Fragment {

    ArrayAdapter<String> mStoriesAdapter;

    public StoriesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // Need to specify country when using zip code
        final String QUERY_COUNTRY = ",USA";

        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("94105" + QUERY_COUNTRY);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Dummy data for` ListView
        ArrayList<String> storiesArray = new ArrayList<String>();
        storiesArray.add("Summer Camp Scholarships for Children with Diabetes");
        storiesArray.add("World Without Meningitis");
        storiesArray.add("Community Health and Data Departments Form New Initiative");

        // Create an ArrayAdapter with the dummy data
        // ArrayAdapter takes data from a source (storiesArray) and uses
        // it to populate the ListView it's attached to.
        mStoriesAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_stories, // name of the layout ID.
                        R.id.list_item_stories_textview, // ID of the TextView to populate.
                        storiesArray
                );

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        // Get a reference to ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_stories);
        listView.setAdapter(mStoriesAdapter);

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        /**
         * Take the String representing the complete stories in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String storiesJsonStr)
                throws JSONException {

            final String OWM_STORY_LIST = "stories";
            final String OWM_STORY_TITLE = "title";

            JSONObject storiesJson = new JSONObject(storiesJsonStr);

            JSONArray storiesArray = storiesJson.getJSONArray(OWM_STORY_LIST);

            String[] resultStrs = new String[storiesArray.length()];
            for(int i = 0; i < storiesArray.length(); i++) {
                String title;

                JSONObject storyObject = storiesArray.getJSONObject(i);
                title = storyObject.getString(OWM_STORY_TITLE);

                resultStrs[i] = title;
                Log.v(LOG_TAG, "Story: " + title);
            }
            return resultStrs;

        }

        @Override
        protected String[] doInBackground(String... params) {
            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String storiesJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                URL url = new URL("https://www.vcdramas.com/api/funds/5/stories?&api_key=a3037caa141c596123e11c04d4638891");

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                storiesJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Stories string: " + storiesJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(storiesJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // Return null if error getting or parsing the stories.
            return null;
        }
        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mStoriesAdapter.clear();
                for(String dayStoriesStr : result) {
                    mStoriesAdapter.add(dayStoriesStr);
                }
                // New data is back from the server.  Hooray!
            }
        }
    }
}