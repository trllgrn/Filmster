package filmster.com.example.com.filmster;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

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

/**
 * Created by tgreen on 8/13/15.
 */
public class FilmFragment extends Fragment {

    //private ImageAdapter mImages;
    private FilmAdapter filmAdapter;

    public FilmFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main,container,false);

        GridView theGrid = (GridView) rootView.findViewById(R.id.gridview_fragment);

        //mImages = new ImageAdapter(rootView.getContext());

        filmAdapter = new FilmAdapter(rootView.getContext(),R.id.grid_element,new ArrayList<Film>());

        theGrid.setAdapter(filmAdapter);
        Log.i("MAIN:onCreate", "Adapter bound to Grid with " + filmAdapter.getCount() + "elements");


        theGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Film f = (Film) filmAdapter.getItem(position);
                //Toast.makeText(view.getContext(), "Launching..." + position, Toast.LENGTH_SHORT).show();
                //Launch the detail activity
                Intent detailIntent = new Intent(view.getContext(),DetailActivity.class);
                //Send the array position of the film we want to see details on
                detailIntent.putExtra(getString(R.string.detail_title),f.title);
                detailIntent.putExtra(getString(R.string.detail_synopsis), f.synopsis);
                detailIntent.putExtra(getString(R.string.detail_release), f.release_date);
                detailIntent.putExtra(getString(R.string.detail_poster),f.poster_path);
                detailIntent.putExtra(getString(R.string.detail_id),f.id);
                detailIntent.putExtra(getString(R.string.detail_rating),f.vote_avg);
                //Launch detail activity
                startActivity(detailIntent);
            }
        });
        Log.i("MAIN:onCreate", "Finished onCreate Layout setup");
        Log.i("MAIN:onCreate", "Attempting adapter udpate");
        FetchPostersTask fetchPosters = new FetchPostersTask();
        fetchPosters.execute();
        return rootView;
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
                imageView.setLayoutParams(new GridView.LayoutParams(342, 513));
                imageView.setScaleType(ImageView.ScaleType.CENTER);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            //Get the URL fragment from the mFimList element
            String img_url_fragment = films.get(position).poster_path;
            String img_size = "w342";
            //Build the URL for Picasso to query with
            //http://image.tmdb.org/t/p/w185//nBNZadXqJSdt05SHLqgT0HuC5Gm.jpg
            Uri.Builder img_url = new Uri.Builder();
            img_url.scheme("http");  //// TODO: 8/13/15 Fix these hardcoded values
            img_url.authority("image.tmdb.org");
            img_url.appendPath("t");
            img_url.appendPath("p");
            img_url.appendPath(img_size);
            img_url.appendPath(img_url_fragment);
            try {
                img_url.build();
            } catch (UnsupportedOperationException e){
                Log.e("IMGAdapter:getView", "Could not build img URL " + e);
            }
            Log.i("IMGAdapter:getView", "Attempting to fetch: " + img_url.toString());
            // Use Picasso to load the image into this imageView
            Picasso.with(context)
                    .load(img_url.toString())
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
            Log.i(LOG_TAG, "Film API Response: " + api_response);

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
            ArrayList<Film> theList = new ArrayList<Film>();
            //Try to parse the JSON objects out of the list
            try {
                JSONObject jObject = new JSONObject(api_response);
                JSONArray resultsArray = jObject.getJSONArray("results");
                //Iterate through the results and grab the movie details
                for (int i = 0; i < resultsArray.length(); i++){
                    JSONObject filmResult = resultsArray.getJSONObject(i);
                    Film film = new Film();
                    film.id = filmResult.getString("id");
                    film.poster_path = filmResult.getString("poster_path");
                    //remove leading '/'
                    film.poster_path = film.poster_path.substring(1);
                    film.title = filmResult.getString("title");
                    film.release_date = filmResult.getString("release_date");
                    film.synopsis = filmResult.getString("overview");
                    film.vote_avg = new Float(filmResult.getDouble("vote_average"));
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
            super.onPostExecute(films);
            //Now that we have movies from our API,
            //we can clear our ImageAdapter and refill it with the
            //new images we retrieved
            //mImages.setContent(films);
            //mImages.notifyDataSetChanged();
            filmAdapter.clear();
            filmAdapter.addAll(films);
        }

        private String executeAPICall(URL url) {
            String movieData = null;

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try{
                //// TODO: 8/14/15
                //Need to move this to the FetchWeatherTask since it's already wrapped in try/catch
                //All Url's should be completely build by now.
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
                else {
                    movieData = buffer.toString();
                    Log.i(LOG_TAG, "JSON Response: " + movieData);
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