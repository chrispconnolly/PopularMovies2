package com.example.chrispconnolly.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.example.chrispconnolly.popularmovies.Utility.getMovieJson;

public class MainActivity extends AppCompatActivity {
    private GridView mGridView;
    private JSONArray mMovieJsonArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGridView = (GridView) findViewById(R.id.movie_gridview);
        UpdateMovies();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        if (id == R.id.action_sortbypopularity)
            preferencesEditor.putString("sort", "popular");
        if (id == R.id.action_sortbyrating)
            preferencesEditor.putString("sort", "top_rated");
        if (id == R.id.action_favorites) {
                String favorites = sharedPreferences.getString("favorites", null);
                if(favorites == null) {
                    Toast toast = Toast.makeText(getApplicationContext(), "No favorites have been selected.", Toast.LENGTH_SHORT);
                    toast.show();
                    return false;
                }
                else
                    preferencesEditor.putString("sort", "favorites");
        }
        preferencesEditor.commit();
        UpdateMovies();
        Log.e("Sort: ", sharedPreferences.getString("sort", null));

        return super.onOptionsItemSelected(item);
    }

    private void UpdateMovies(){
        FetchMoviesTask fetchMoviesTask = new FetchMoviesTask();
        fetchMoviesTask.execute();
    }

    public class FetchMoviesTask extends AsyncTask<String, Void, ArrayList<Bitmap>> {

        @Override
        protected void onPostExecute(ArrayList<Bitmap> posterUrls) {
            mGridView.invalidateViews();
            mGridView.setAdapter(new ImageAdapter(getApplicationContext(), posterUrls));
        }
        @Override
        protected ArrayList<Bitmap> doInBackground(String... params) {
            try{
                SharedPreferences sharedPreferences = getDefaultSharedPreferences(getApplicationContext());
                String sortPreference = sharedPreferences.getString("sort", null);
                sortPreference = (sortPreference == null) ? "popular" : sortPreference;
                mMovieJsonArray = new JSONArray();
                if(sortPreference.equals("favorites")) {
                    String[] movieIds = sharedPreferences.getString("favorites", null).split(",");
                        for(int i=0; i< movieIds.length; i++){
                            Uri.Builder builder = new Uri.Builder();
                            builder.scheme("https")
                                .authority("api.themoviedb.org")
                                .appendPath("3")
                                .appendPath("movie")
                                .appendPath(movieIds[i])
                                .appendQueryParameter("api_key", BuildConfig.THE_MOVIE_DATABASE_API_KEY);
                            mMovieJsonArray.put(i, getMovieJson(builder.build().toString()));
                    }
                }
                else {
                    Uri.Builder builder = new Uri.Builder();
                    builder.scheme("https")
                            .authority("api.themoviedb.org")
                            .appendPath("3")
                            .appendPath("movie")
                            .appendPath(sortPreference)
                            .appendQueryParameter("api_key", BuildConfig.THE_MOVIE_DATABASE_API_KEY);

                    mMovieJsonArray = getMovieJson(builder.build().toString()).getJSONArray("results");
                }
                String[] posterUrls = getMoviesDataFromJson();
                ArrayList<Bitmap> bitmapArrayList = new ArrayList<Bitmap>();
                for(String posterUrl : posterUrls)
                    bitmapArrayList.add(getBitmap(posterUrl));
                return bitmapArrayList;
            } catch (Exception e) {
                Log.e("FetchMoviesTask", "Error ", e);
                return null;
            }
        }

        private String[] getMoviesDataFromJson()
                throws JSONException {
            String[] resultStrs = new String[mMovieJsonArray.length()];
            for(int i = 0; i < mMovieJsonArray.length(); i++) {
                JSONObject jsonObject = mMovieJsonArray.getJSONObject(i);
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority("image.tmdb.org")
                        .appendPath("t")
                        .appendPath("p")
                        .appendPath("w185")
                        .appendPath(jsonObject.getString("poster_path").substring(1));
                String posterUrl = builder.build().toString();
                resultStrs[i] = posterUrl;
            }
            return resultStrs;
        }

        public Bitmap getBitmap(String posterUrl) {
            try {
                HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(posterUrl).openConnection();
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();
                InputStream inputStream = httpURLConnection.getInputStream();
                return BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                Log.e("FetchMoviesTask", "Error getting image", e);
                return null;
            }
        }
    }
    public class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<Bitmap> mBitmapArrayList;

        public ImageAdapter(Context c, ArrayList<Bitmap> bitmapArrayList) {
            mContext = c;
            mBitmapArrayList = bitmapArrayList;
        }
        public int getCount() { return mBitmapArrayList.size();}
        public Object getItem(int position) { return null;}
        public long getItemId(int position) {return 0;}

        public View getView(final int position, View convertView, ViewGroup parent) {
            ImageView imageView = new ImageView(mContext);
            final Bitmap bitmap = mBitmapArrayList.get(position);
            imageView.setImageBitmap(bitmap);
            imageView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        JSONObject movieJson = (JSONObject) mMovieJsonArray.get(position);
                        Intent detailIntent = new Intent(mContext, DetailActivity.class)
                                .putExtra("movieJson", movieJson.toString())
                                .putExtra("movieBitmap", bitmap);
                        startActivity(detailIntent);
                    }
                    catch(JSONException e){}
                }
            });
            return imageView;
        }
    }
}