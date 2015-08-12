package filmster.com.example.com.filmster;

import android.content.Context;
import android.graphics.Movie;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GridView theGrid = (GridView) findViewById(R.id.gridview_main);

        ImageAdapter images = new ImageAdapter(getBaseContext());
        theGrid.setAdapter(images);
        theGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getBaseContext(),"Fetching..." + position,Toast.LENGTH_SHORT).show();
                FetchPostersTask posterize = new FetchPostersTask();
                posterize.execute();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<Integer> mThumbs;


        public ImageAdapter(Context c ){
            mContext = c;
            mThumbs = new ArrayList<Integer>();
            mThumbs.add(R.drawable.sample_0);
            mThumbs.add(R.drawable.sample_1);
            mThumbs.add(R.drawable.sample_2);
            mThumbs.add(R.drawable.sample_3);
            mThumbs.add(R.drawable.sample_4);
            mThumbs.add(R.drawable.sample_5);
            mThumbs.add(R.drawable.sample_6);
            mThumbs.add(R.drawable.sample_7);
        }

        @Override
        public int getCount() {
            return mThumbs.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(270,
                        480));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setImageResource(mThumbs.get(position));
            return imageView;
        }
    }

    private class FetchPostersTask extends AsyncTask<String,Void,Void> {

        private final String LOG_TAG = FetchPostersTask.class.getSimpleName();

        @Override
        protected Void doInBackground(String... params) {
            //data will come in as a String structure
            //we'll manually set the attributes for one example first
            //later we will send

            Uri.Builder pop_movies_request = new Uri.Builder();
            pop_movies_request.scheme(getString(R.string.movie_db_sheme));
            pop_movies_request.authority(getString(R.string.movie_db_authority));
            pop_movies_request.appendPath(getString(R.string.movie_db_version));
            pop_movies_request.appendPath(getString(R.string.movie_db_discover));
            pop_movies_request.appendPath(getString(R.string.movie_db_movie));
            // TODO: 8/11/15  :
            //Add logic here make sure we get the right parameters based on
            //SharedPrefs setting
            //In order to make keep the integrity of themoviedb.or API and my own account
            //I have removed my own API key from this repo.  If you want to rebuild this code from
            //a cloned repo, you will have to add a resource file containing your own API key to
            //make this code work.
//            <resources>
//            <string name="movie_db_api_key">xxxYourApIKeyxxx</string>
//            </resources>

            String sort_order = getString(R.string.movie_db_sort_val_popular);
            pop_movies_request.appendQueryParameter(getString(R.string.movie_db_sort_param),sort_order);
            pop_movies_request.appendQueryParameter(getString(R.string.movie_db_api_param),
                    getString(R.string.movie_db_api_key));
            URL url = null;

            try {
                url = new URL(pop_movies_request.build().toString());
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG,"URL Creation ERROR",e);
            }
            //Make the call
            String api_response = executeAPICall(url);
            Log.i(LOG_TAG, "Movie API Response: " + api_response);

            //movies array to hold the returned movie objects
            ArrayList<Movie> movies = new ArrayList<Movie>();

            if(api_response == null) {
                Log.e(LOG_TAG, "JSON String was empty!");
            }
            else {
                //Try to parse the returned JSON string
                // Extract movies from response

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        private String executeAPICall(URL url) {
            String movieData = null;

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try{

                // Possible parameters are available at OWM's forecast API page, at
                // Create the request to the API, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    movieData = null;
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
                    movieData = null;
                }
                movieData = buffer.toString();
                Log.i(LOG_TAG,"JSON Response: " + movieData);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                movieData = null;
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
            return movieData;
        }
    }
}


