package filmster.com.example.com.filmster;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {
    private ClipAdapter trailerAdapter = null;
    private Film thisFilm = null;
    private ArrayList<Review> reviews = null;
    private ArrayList<Clip> videos = null;
    private LinearLayout videoList = null;


    public DetailActivityFragment() {
    }


    private void getMovieDetails() {
        //Need to check for Internet Connectivity before attempting execute the update
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo myNet = cm.getActiveNetworkInfo();
        if (myNet == null || !myNet.isConnected()) {
            Toast.makeText(this.getActivity(),
                    "No internet detected. Please check connections.",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            FetchDetailsTask fetchDetails = new FetchDetailsTask();
            try {
                fetchDetails.execute();
            }
            catch(Exception e) {
                Toast.makeText(this.getActivity(),"Unknown error occured. Please restart.",Toast.LENGTH_LONG);
            }

        }
    }


    private class FetchDetailsTask extends AsyncTask<Void,Void,Void>{
        private final String LOG_TAG = FetchDetailsTask.class.getSimpleName();
        final String MDB_JSON_RESULTS = "results";

        @Override
        protected Void doInBackground(Void... params) {
            //Build the URL to query themoviedb.org API with
            Uri.Builder details_request = new Uri.Builder();
            details_request.scheme(getString(R.string.movie_db_scheme));
            details_request.authority(getString(R.string.movie_db_authority));
            details_request.appendPath(getString(R.string.movie_db_version));
            details_request.appendPath(getString(R.string.movie_db_movie));
            details_request.appendPath(thisFilm.id);
            details_request.appendQueryParameter(getString(R.string.movie_db_api_param),
                    getString(R.string.movie_db_api_key));
            details_request.appendQueryParameter("append_to_response","trailers");

            URL url = null;

            try {
                url = new URL(details_request.build().toString() +  ",reviews");
                //appended reviews parameter
                //this is pretty horrendous but I coudln't figure out a way to make
                //it work with the uri builder since there are multiple values
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG,"URL Creation ERROR",e);
            }

            Log.i(LOG_TAG,"Built URL: " + url);

            //Make the call
            String api_response = executeAPICall(url);

            //Parse the API response
            Log.i("FetchTask:doInBack", "JSON Reply: " + api_response);
            //Extract extra movie details
            //parseExtraDetails(api_response);
            //Extract movie reviews
            reviews = parseReviews(api_response);
            //Extract movie trailers
            videos = parseTrailer(api_response);

            return null;
        }

        private ArrayList parseTrailer(String response) {
            //JSON parsing constants
            final String MDB_JSON_TRAILERS = "trailers";
            final String MDB_JSON_TRAILER_SOURCE = "youtube";
            final String MDB_JSON_TRAILER_NAME = "name";
            final String MDB_JSON_TRAILER_KEY = "source";
            final String MDB_JSON_TRAILER_TYPE = "type";

            //Results array
            ArrayList<Clip> clipList = new ArrayList<>();

            //Get a JSON object from response
            try {
                //Get a JSON object from response
                JSONObject movieResponse = new JSONObject(response);
                //Get the trailers object
                JSONObject trailersObj = movieResponse.getJSONObject(MDB_JSON_TRAILERS);
                Log.i(LOG_TAG,"trailers JSON: " + trailersObj.toString());
                //Get the JSON Array for youtube
                JSONArray youtubeArray = trailersObj.getJSONArray(MDB_JSON_TRAILER_SOURCE);
                Log.i(LOG_TAG, "youtube list JSON:" +  youtubeArray.toString());
                //Create a new Clip entry for each video
                if (youtubeArray.length() > 0) {
                    for (int i = 0; i < youtubeArray.length(); i++) {
                        JSONObject videoClip = youtubeArray.getJSONObject(i);
                        //Create a new Clip object to save this response data
                        Clip clipEntry = new Clip();
                        clipEntry.key = videoClip.getString(MDB_JSON_TRAILER_KEY);
                        clipEntry.name = videoClip.getString(MDB_JSON_TRAILER_NAME);
                        clipEntry.type = videoClip.getString(MDB_JSON_TRAILER_TYPE);
                        clipList.add(clipEntry);
                    }

                    Log.i(LOG_TAG,"Captured clips: " + clipList.toString());
                }

            }catch (Exception e) {
                Log.e(LOG_TAG,"Trouble with api response: " + e);
            }

            return clipList;
        }

        private ArrayList parseReviews(String response) {
            //JSON parsing constants

            final String MDB_JSON_REVIEWS = "reviews";
            final String MDB_JSON_AUTHOR = "author";
            final String MDB_JSON_CONTENT = "content";
            final String MDB_JSON_URL = "url";

            ArrayList<Review> apiReviews = new ArrayList<>();

            try {
                JSONObject movieResponse = new JSONObject(response);
                JSONObject apiReview = movieResponse.getJSONObject(MDB_JSON_REVIEWS);
                JSONArray reviewsArray = apiReview.getJSONArray(MDB_JSON_RESULTS);

                if (reviewsArray.length() > 0) {
                    for (int i = 0; i < reviewsArray.length(); i++) {
                        JSONObject reviewObject = reviewsArray.getJSONObject(i);
                        Review review = new Review();
                        review.author = reviewObject.getString(MDB_JSON_AUTHOR);
                        review.content = reviewObject.getString(MDB_JSON_CONTENT);
                        review.url = reviewObject.getString(MDB_JSON_URL);
                        apiReviews.add(review);
                    }
                }

            } catch (Exception e) {
                Log.e(LOG_TAG,"Trouble with api response: " + e);
            }

            return apiReviews;
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

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //Back on the UI thread
            Log.i("FetchDetails", "Trailers: " + videos.toString());

            //Since we updated the backing videos data structure
            //We might need to call dataSetChanged on the adapter
            trailerAdapter.clear();
            trailerAdapter.addAll(videos);
            trailerAdapter.notifyDataSetChanged();
            Log.i("FetchDetails", "trailer adapter count: " + trailerAdapter.getCount());
        }
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Initiate calls to get reviews from the API server
        //Get the trailer from the server
        if (savedInstanceState != null) {
            //recovering from being hidden
            thisFilm = savedInstanceState.getParcelable(getString(R.string.detail_film_object));
            //TODO:
            //Recover additional details fetched from the API
            ArrayList<Clip> savedVids = savedInstanceState.getParcelableArrayList(getString(R.string.detail_fetched_vids));
            if (savedVids != null) {
                videos = savedVids;
            }
            else {
                //Somehow we saved an null list
                videos = new ArrayList<>();

                if (thisFilm != null) {
                    getMovieDetails();
                }
                else {
                    Log.i("Details:onCreate","Somehow we don't have a movie. We should probably kill ourselves.");
                }

            }
        }
        else {
            //this is the first time we're launched
            //need to retrieve the Film object from the intent that launched us.
            Intent intent = getActivity().getIntent();
            if (intent.hasExtra(getString(R.string.detail_film_object))) {
                thisFilm = intent.getExtras().getParcelable(getString(R.string.detail_film_object));

                //Now that we have our film, let's grab the other info from the api

                //initialize the clips array
                videos = new ArrayList<>();

                if (thisFilm != null) {
                    getMovieDetails();
                }
                else {
                    Log.i("Details:onCreate","Film intent was null! Oh no!");
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        ImageView posterView = (ImageView) rootView.findViewById(R.id.film_detail_poster);
        TextView titleTextView = (TextView) rootView.findViewById(R.id.film_title);
        TextView synopsisTextView = (TextView) rootView.findViewById(R.id.film_synopsis);
        TextView releaseView = (TextView) rootView.findViewById(R.id.film_release);
        TextView ratingView = (TextView) rootView.findViewById(R.id.film_rating);
        //videoList = (LinearLayout) rootView.findViewById(R.id.detail_trailers_list);
        ListView videoListView = (ListView) rootView.findViewById(R.id.detail_trailers_listview);
        ListView reviewsListView = (ListView) rootView.findViewById(R.id.detail_reviews_listview);

        trailerAdapter = new ClipAdapter(rootView.getContext(),R.layout.fragment_detail,videos);

        videoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                sendToYoutube(view.getContext(), trailerAdapter.getItem(position));
            }
        });

        //Bind the adapter to the ListView
        videoListView.setAdapter(trailerAdapter);

        titleTextView.setText(thisFilm.title);
        synopsisTextView.setText(thisFilm.synopsis);
        releaseView.setText(thisFilm.release_date);
        ratingView.setText(thisFilm.vote_avg.toString());

        //Load up the Poster
        Picasso.with(rootView.getContext()).load(thisFilm.poster_path).into(posterView);

        Log.i("Detail:onCreateView", "Finished building the detail layout.");

        return rootView;
    }

    public void sendToYoutube(Context context, Clip video) {
        Intent videoIntent = new Intent();
        Uri videoURL = null;
        try {
            Uri.Builder trailerPath = new Uri.Builder();
            videoURL = trailerPath.scheme("http")
                                  .authority("www.youtube.com")
                                  .appendPath("watch")
                                  .appendQueryParameter("v",video.key)
                                  .build();
        }catch (Exception e){
            Log.e("sendToU: ", "could not build trailer Uri " + e);
        }

        videoIntent.setAction(Intent.ACTION_VIEW);
        videoIntent.setData(videoURL);
        if(videoIntent.resolveActivity(context.getPackageManager()) != null) {
            startActivity(videoIntent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Preparing to be destroyed
        //Save this movie
        outState.putParcelable(getString(R.string.detail_film_object),thisFilm);

        //Save the videos list
        outState.putParcelableArrayList(getString(R.string.detail_fetched_vids),videos);

        //Save the reviews List
        //outState.putParcelableArrayList(reviews);
    }

    private class ClipAdapter extends ArrayAdapter<Clip> {
        private Context context;
        private int layoutId;
        private ArrayList<Clip> clips;

        public ClipAdapter(Context context, int resource, List<Clip> objects) {
            super(context, resource, objects);
            this.context = context;
            this.layoutId = resource;
            this.clips = new ArrayList(objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            String trailer_template_url = "http://img.youtube.com/vi/%s/mqdefault.jpg";

            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(context);
                imageView.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setScaleType(ImageView.ScaleType.CENTER);
            } else {
                imageView = (ImageView) convertView;
            }

            Clip thisClip = getItem(position);
            String trailer_url = String.format(trailer_template_url, thisClip.key);

            Log.i("ClipAdapter:getView", "Set trailer url: " + trailer_url);

            // Use Picasso to load the image into this imageView
            Picasso.with(context)
                    .load(trailer_url)
                    .placeholder(R.drawable.sample_0)
                    .error(R.drawable.sample_3)
                    .into(imageView);
            return imageView;
        }
    }

    private class ReviewAdapter extends ArrayAdapter<Review> {
        public ReviewAdapter(Context context, int resource, List<Review> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return super.getView(position, convertView, parent);
        }
    }
}
