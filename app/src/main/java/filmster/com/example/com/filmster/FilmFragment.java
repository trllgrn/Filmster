package filmster.com.example.com.filmster;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by tgreen on 8/13/15.
 */
public class FilmFragment extends Fragment {

    private ArrayList<Film> myFlix;

    private FilmAdapter filmAdapter;

    private final String MOVIE_LIST_KEY = "POPULAR";

    public FilmFragment() {

    }

    public class voteCompare implements Comparator<Film> {
        @Override
        public int compare(Film lhs, Film rhs) {
            return lhs.vote_avg.compareTo(rhs.vote_avg);
        }
    }

    public class popCompare implements Comparator<Film> {
        @Override
        public int compare(Film lhs, Film rhs) {
            return lhs.popularity.compareTo(rhs.popularity);
        }
    }

    private void updateMovies() {

        //Need to check for Internet Connectivity before attempting execute the update
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo myNet = cm.getActiveNetworkInfo();
        if (myNet == null || !myNet.isConnected()) {
            Toast.makeText(this.getActivity(),
                           "No internet detected. Please check connections.",
                           Toast.LENGTH_SHORT).show();
        }
        else {
            FetchPostersTask fetchPosters = new FetchPostersTask();
            try {
                fetchPosters.execute();
            }
            catch(Exception e) {
                Toast.makeText(this.getActivity(),"Unknown error occured. Please restart.",Toast.LENGTH_LONG);
            }

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (myFlix != null) {
            //Let's check SharedPreferences to see if we need to sort the collection
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String sortPref = settings.getString(getString(R.string.pref_sort_order_key),
                    getString(R.string.pref_sort_order_default));

            if (sortPref.equals(getString(R.string.pref_sort_order_rating))) {
                //Sort the arrayList by vote_average
                Collections.sort(myFlix, Collections.reverseOrder(new voteCompare()));
            }
            else {
                //Sort the arrayList by popularity
                Collections.sort(myFlix, Collections.reverseOrder(new popCompare()));
            }
        }

        filmAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //Fetch Movies or Restore from Bundle
        //Check to see if there's data to restore
        if (savedInstanceState != null) {
            //Restore the Films from the Bundle
            ArrayList<Film> savedMovies = savedInstanceState.getParcelableArrayList(MOVIE_LIST_KEY);
            if (savedMovies != null) {
                myFlix = savedMovies;
            }
            else {
                //Somehow we didn't save anything.
                //Better initialize myFlix
                myFlix = new ArrayList<>();
                //Attempt to get some movies now
                updateMovies();
            }

        }
        else {
            //This is the first time we're creating the activity
            //So, go get the movies already!
            myFlix = new ArrayList<Film>();
            updateMovies();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save what's in the adapter, if anything
        ArrayList<Film> theMovies = filmAdapter.films;
        if (!theMovies.isEmpty()) {
            outState.putParcelableArrayList(MOVIE_LIST_KEY,theMovies);
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_main,container,false);

        GridView theGrid = (GridView) rootView.findViewById(R.id.gridview_fragment);

        filmAdapter = new FilmAdapter(rootView.getContext(),R.id.grid_element, myFlix);

        theGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                sendToDetail(view.getContext(), filmAdapter.getItem(position));
            }
        });

        //Bind the adapter to the View
        theGrid.setAdapter(filmAdapter);
        return rootView;
    }

    public void sendToDetail(Context context, Film f) {
        //Launch the detail activity
        Intent detailIntent = new Intent(context, DetailActivity.class);
        detailIntent.putExtra(getString(R.string.detail_film_object),f);

        startActivity(detailIntent);
    }

    private class FilmAdapter extends ArrayAdapter<Film> {
        private Context context;
        private int layoutResourceId;
        private ArrayList<Film> films = null;

        public FilmAdapter(Context context, int layoutResourceId, ArrayList<Film> data) {
            super(context,layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.films = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(context);
                imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView = (ImageView) convertView;
            }

            Film thisMovie = getItem(position);

            // Use Picasso to load the image into this imageView
            Picasso.with(context)
                    .load(thisMovie.poster_path)
                    .error(R.drawable.sample_3)
                    .into(imageView);
            return imageView;
        }
    }


    private class FetchPostersTask extends AsyncTask<Void,Void,ArrayList<Film>> {

        private final String LOG_TAG = FetchPostersTask.class.getSimpleName();

        @Override
        protected ArrayList<Film> doInBackground(Void... params) {
            //Build the URL to query themoviedb.org API with
            Uri.Builder pop_movies_request = new Uri.Builder();
            pop_movies_request.scheme(getString(R.string.movie_db_scheme));
            pop_movies_request.authority(getString(R.string.movie_db_authority));
            pop_movies_request.appendPath(getString(R.string.movie_db_version));
            pop_movies_request.appendPath(getString(R.string.movie_db_discover));
            pop_movies_request.appendPath(getString(R.string.movie_db_movie));

            //In order to make keep the integrity of themoviedb.org API and my own account
            //I have removed my own API key from this repo.  If you want to rebuild this code from
            //a cloned repo, you will have to add a string resource file containing your own API key to
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
            //movies array to hold the returned movie objects
            ArrayList<Film> movies = null;

            if(api_response == null) {
                Log.e(LOG_TAG, "JSON String was empty!");
            }
            else {
                //Try to parse the returned JSON string
                // Extract movies from response
                movies = parseDiscoverResponse(api_response);
            }

            return movies;
        }

        private ArrayList<Film> parseDiscoverResponse(String api_response) {
            //API URL queries
            final String MDB_URL_SCHEME = "http";
            final String MDB_URL_AUTH = "image.tmdb.org";
            final String MDB_URL_PATH_T = "t";
            final String MDB_URL_PATH_P = "p";
            final String MDB_URL_IMG = "w342";

            //JSON Parsing constants
            final String MDB_JSON_RESULTS = "results";
            final String MDB_JSON_ID = "id";
            final String MDB_JSON_TITLE = "title";
            final String MDB_JSON_RELEASE = "release_date";
            final String MDB_JSON_PLOT = "overview";
            final String MDB_JSON_RATING = "vote_average";
            final String MDB_JSON_POP = "popularity";


            ArrayList<Film> theList = new ArrayList<>();
            //Try to parse the JSON objects out of the list
            try {
                JSONObject jObject = new JSONObject(api_response);
                JSONArray resultsArray = jObject.getJSONArray(MDB_JSON_RESULTS);
                //Iterate through the results and grab the movie details
                for (int i = 0; i < resultsArray.length(); i++){
                    JSONObject filmResult = resultsArray.getJSONObject(i);
                    Film film = new Film();
                    film.id = filmResult.getString(MDB_JSON_ID);
                    film.poster_path = filmResult.getString("poster_path");
                    if (film.poster_path != null) {
                        //remove leading '/' from path fragment
                        film.poster_path = film.poster_path.substring(1);
                        //Build the full poster url
                        Uri.Builder img_url = new Uri.Builder();
                        img_url.scheme(MDB_URL_SCHEME);
                        img_url.authority(MDB_URL_AUTH);
                        img_url.appendPath(MDB_URL_PATH_T);
                        img_url.appendPath(MDB_URL_PATH_P);
                        img_url.appendPath(MDB_URL_IMG);
                        img_url.appendPath(film.poster_path);
                        img_url.build();
                        film.poster_path = img_url.toString();
                    }

                    film.title = filmResult.getString(MDB_JSON_TITLE);
                    film.release_date = filmResult.getString(MDB_JSON_RELEASE);
                    film.synopsis = filmResult.getString(MDB_JSON_PLOT);
                    film.vote_avg = new Float(filmResult.getDouble(MDB_JSON_RATING));
                    film.popularity = filmResult.getDouble(MDB_JSON_POP);
                    theList.add(film);
                }

            }
            catch (JSONException e) {
                Log.e(LOG_TAG,"Trouble with api response: " + e);
            }

            return theList;
        }

        @Override
        protected void onPostExecute(ArrayList<Film> films) {
            if (films != null) {
                super.onPostExecute(films);
                //Now that we have movies from our API,
                //we can clear our ImageAdapter and refill it with the
                //new images we retrieved

                filmAdapter.clear();
                filmAdapter.addAll(films);
            }
        }

        private String executeAPICall(URL url) {
            String movieData = null;

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try{

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
                else {
                    movieData = buffer.toString();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the movie data, there's no point in attempting
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