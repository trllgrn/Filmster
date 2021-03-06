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
import java.util.HashSet;

/**
 * Created by tgreen on 8/13/15.
 */
public class FilmFragment extends Fragment {

    private ArrayList<Film> myFlix;

    private ArrayList<Film> favFlix;

    private FilmAdapter filmAdapter;

    private final String MOVIE_LIST_KEY = "POPULAR";
    private final String FAVS_LIST_KEY = "FAVORITES";
    private final String MOVIE_ORDER = "SORT";

    private String sortOrder = null;

    public FilmFragment() {

    }

    public interface Callback {
        void onItemSelected(Film detailFilm);
    }

    private void updateMovies(String sortOrder) {

        //Need to check for Internet Connectivity before attempting execute the update
        ConnectivityManager cm = (ConnectivityManager) getActivity().
                                                       getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo myNet = cm.getActiveNetworkInfo();
        if (myNet == null || !myNet.isConnected()) {
            Toast.makeText(this.getActivity(),
                           "No internet detected. Please check connections.",
                           Toast.LENGTH_SHORT).show();
        }
        else {
            FetchPostersTask fetchPosters = new FetchPostersTask();
            try {
                fetchPosters.execute(sortOrder);
            }
            catch(Exception e) {
                Toast.makeText(this.getActivity(),"Unknown error occured. Please restart.",Toast.LENGTH_LONG);
            }

        }
    }

    private void getFavorites() {
        //Need to check for Internet Connectivity before attempting execute the update
        ConnectivityManager cm = (ConnectivityManager) getActivity().
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo myNet = cm.getActiveNetworkInfo();
        if (myNet == null || !myNet.isConnected()) {
            Toast.makeText(this.getActivity(),
                    "No internet detected. Please check connections.",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            FetchFavoritesTask fetchFavs = new FetchFavoritesTask();
            try {
                fetchFavs.execute();
            }
            catch(Exception e) {
                Toast.makeText(this.getActivity(),"Unknown error occured. Please restart.",Toast.LENGTH_LONG);
            }

        }
    }

    private class FetchFavoritesTask extends AsyncTask<Void,Void,Void> {

        private final String LOG_TAG = FetchFavoritesTask.class.getSimpleName();

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (favFlix != null) {
                filmAdapter.clear();
                filmAdapter.addAll(favFlix);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            //Query the API for all of the movies in our favorites list by id
            //First we need to retrieve the set of movies from SharedPrefs


            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            HashSet<String> favs = (HashSet) settings.getStringSet(getString(R.string.pref_fav_key),
                                                                   new HashSet<String>());

            //Build the API call
            Uri.Builder fav_movies_request = new Uri.Builder();
            fav_movies_request.scheme(getString(R.string.movie_db_scheme));
            fav_movies_request.authority(getString(R.string.movie_db_authority));
            fav_movies_request.appendPath(getString(R.string.movie_db_version));
            fav_movies_request.appendPath(getString(R.string.movie_db_movie));

            Uri movieDbBase = fav_movies_request.build();

            if (!favs.isEmpty()) {
                //Iterate through the list of favorite films from the hash set
                for (String favId : favs) {
                    Uri.Builder favBuilder = movieDbBase.buildUpon();

                    //Add the movie id to the query path
                    favBuilder.appendPath(favId);

                    //Add the api key to the query.
                    favBuilder.appendQueryParameter(getString(R.string.movie_db_api_param),
                            getString(R.string.movie_db_api_key));


                    URL url = null;

                    try {
                        url = new URL(favBuilder.build().toString());
                    } catch (MalformedURLException e) {
                        Log.e(LOG_TAG, "URL Creation ERROR", e);
                    }
                    //Make the call
                    String api_response = executeAPICall(url);

                    Film fetchedFav = null;

                    if (api_response != null) {
                        fetchedFav = parseMovieDetails(api_response);

                        //Mark as a favorite
                        fetchedFav.favorite = true;

                        //Add to favFlix
                        if (!favFlix.contains(fetchedFav)) {
                            favFlix.add(fetchedFav);
                        }
                    }

                }

            }

            return null;
        }

        private Film parseMovieDetails(String response) {
            //API URL queries
            final String MDB_URL_SCHEME = "http";
            final String MDB_URL_AUTH = "image.tmdb.org";
            final String MDB_URL_PATH_T = "t";
            final String MDB_URL_PATH_P = "p";
            final String MDB_URL_IMG = "w342";

            //JSON Parsing constants
            final String MDB_JSON_ID = "id";
            final String MDB_JSON_TITLE = "title";
            final String MDB_JSON_RELEASE = "release_date";
            final String MDB_JSON_PLOT = "overview";
            final String MDB_JSON_RATING = "vote_average";
            final String MDB_JSON_POP = "popularity";

            Film aFav = new Film();

            try {

                JSONObject movieResponse = new JSONObject(response);
                aFav.id = movieResponse.getString(MDB_JSON_ID);
                aFav.title = movieResponse.getString(MDB_JSON_TITLE);
                aFav.release_date = movieResponse.getString(MDB_JSON_RELEASE);
                aFav.synopsis = movieResponse.getString(MDB_JSON_PLOT);
                aFav.vote_avg = new Float(movieResponse.getDouble(MDB_JSON_RATING));
                aFav.popularity = movieResponse.getDouble(MDB_JSON_POP);
                aFav.poster_path = movieResponse.getString("poster_path");
                if (aFav.poster_path != null) {
                    //remove leading '/' from path fragment
                    aFav.poster_path = aFav.poster_path.substring(1);
                    //Build the full poster url
                    Uri.Builder img_url = new Uri.Builder();
                    img_url.scheme(MDB_URL_SCHEME);
                    img_url.authority(MDB_URL_AUTH);
                    img_url.appendPath(MDB_URL_PATH_T);
                    img_url.appendPath(MDB_URL_PATH_P);
                    img_url.appendPath(MDB_URL_IMG);
                    img_url.appendPath(aFav.poster_path);
                    img_url.build();
                    aFav.poster_path = img_url.toString();
                }

            } catch (Exception e) {
                Log.e(LOG_TAG,"Trouble with api response: " + e);
            }

            return aFav;
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

    @Override
    public void onStart() {
        super.onStart();
        if (filmAdapter != null) {
            //Let's check SharedPreferences to see if we need to sort the collection
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String sortPref = settings.getString(getString(R.string.pref_sort_order_key),
                    getString(R.string.pref_sort_order_default));

            if (sortPref.equals(getString(R.string.pref_sort_order_rating)) ||
                    sortPref.equals(getString(R.string.pref_sort_order_popular))) {

                //sortOrder should have been restored from the Bundle. So it's value is the
                //previous sort order.  We check it against the current SharedPrefs setting to
                //see if we need to sort.
                if (!myFlix.isEmpty() && sortOrder.equals(sortPref)) {
                    //set the adapter content using content from Bundle
                    filmAdapter.clear();
                    filmAdapter.addAll(myFlix);
                }
                else {
                    //Call the API, update the sort Order
                    sortOrder = sortPref;
                    updateMovies(sortPref);
                }

            }
            else if (sortPref.equals(getString(R.string.pref_sort_order_fav))) {
                //Check favFlix
                if (sortOrder.equals(sortPref)) {
                    //set the adapter using content from Bundle
                    filmAdapter.clear();
                    filmAdapter.addAll(favFlix);
                }
                else {
                    //favflix content may be out of date. Better refresh
                    sortOrder = sortPref;
                    getFavorites();
                }
            }
        }


    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //Fetch saved Favorites from Bundle
        //Let's check SharedPreferences to see if we need to sort the collection
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortPref = settings.getString(getString(R.string.pref_sort_order_key),
                getString(R.string.pref_sort_order_default));


        //Fetch Movies or Restore from Bundle
        //Check to see if there's data to restore
        if (savedInstanceState != null) {
            //Restore the Films from the Bundle
            ArrayList<Film> savedMovies = savedInstanceState.getParcelableArrayList(MOVIE_LIST_KEY);

            //Restore the favorite films too
            ArrayList<Film> savedFavs = savedInstanceState.getParcelableArrayList(FAVS_LIST_KEY);

            //Restore the sort order
            sortOrder = savedInstanceState.getString(MOVIE_ORDER);

            if (savedFavs != null) {
                favFlix = savedFavs;
            }

            if (savedMovies != null) {
                myFlix = savedMovies;
            }
            else {
                //Somehow we didn't save anything.
                //Better initialize myFlix
                myFlix = new ArrayList<>();
                //Attempt to get some movies now
                updateMovies(sortPref);
            }

        }
        else {
            //This is the first time we're creating the activity
            //So, go get the movies already!
            myFlix = new ArrayList<>();
            //initialize the favorites
            favFlix = new ArrayList<>();

            if (sortPref.equals(getString(R.string.pref_sort_order_popular)) ||
                    sortPref.equals(getString(R.string.pref_sort_order_rating))) {

                sortOrder = sortPref;
                updateMovies(sortPref);
            }
            else {
                //Retreieve our favorites in a background thread
                sortOrder = sortPref;
                getFavorites();
            }
        }
    }



    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the list of movies in the grid
        if (myFlix != null) {
            outState.putParcelableArrayList(MOVIE_LIST_KEY,myFlix);
        }

        //Save the list of fav film objects for possible display later
        if (favFlix != null) {
            outState.putParcelableArrayList(FAVS_LIST_KEY, favFlix);
        }

        //Save the sortOrder
        if (sortOrder != null) {
            outState.putString(MOVIE_ORDER, sortOrder);
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

        filmAdapter = new FilmAdapter(rootView.getContext(),R.id.grid_element, new ArrayList<Film>());

        theGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //sendToDetail(view.getContext(), filmAdapter.getItem(position));
                ((Callback)getActivity()).onItemSelected(filmAdapter.getItem(position));
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

        public void setFilms(ArrayList<Film> filmsToUse) {
            films = filmsToUse;
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(context);
                imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                    ViewGroup.LayoutParams.MATCH_PARENT));
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


    private class FetchPostersTask extends AsyncTask<String,Void,ArrayList<Film>> {

        private final String LOG_TAG = FetchPostersTask.class.getSimpleName();

        @Override
        protected ArrayList<Film> doInBackground(String... params) {
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


            //Set the sort order param based on the parameter
            String sort_order = getString(R.string.pref_sort_order_popular);

            if (params.length > 0) {
                sort_order = params[0];
            }

            String sort_order_param = null;

            if (sort_order.equals(getString(R.string.pref_sort_order_popular))) {
                sort_order_param = getString(R.string.movie_db_sort_val_popular);
            }
            else if (sort_order.equals(getString(R.string.pref_sort_order_rating))) {
                sort_order_param = getString(R.string.movid_db_sort_val_hi_rated);
            }


            pop_movies_request.appendQueryParameter(getString(R.string.movie_db_sort_param),sort_order_param);
            pop_movies_request.appendQueryParameter(getString(R.string.movie_db_api_param),
                    getString(R.string.movie_db_api_key));

            //add the qualification that the movies have at least 50 reviews
            pop_movies_request.appendQueryParameter("vote_count.gte","50");

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

            //Set up the favorites
            setFavorites(theList);

            return theList;
        }

        protected void setFavorites(ArrayList<Film> list) {
            //Get the favorite list from sharedPrefs and see who's on it
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            //Retreive favorites list
            HashSet<String> favs = (HashSet) settings.getStringSet(getString(R.string.pref_fav_key),new HashSet<String>() );

            if (favs != null) {
                if (!favs.isEmpty()) {
                    //traverse through the list of returned movies and determine if they are in the set
                    for (Film f : list) {
                        if (favs.contains(f.id)) {
                            f.favorite = true;
                        }
                    }
                }
            }
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
                myFlix = films;

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