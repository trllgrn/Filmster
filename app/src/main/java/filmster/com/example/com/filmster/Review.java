package filmster.com.example.com.filmster;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by tgreen on 9/7/15.
 */
public class Review implements Parcelable {
    public String author;
    public String content;
    public String url;

    public Review() {
        author = null;
        content = null;
        url = null;
    }

    protected Review(Parcel in) {
        author = in.readString();
        content = in.readString();
        url = in.readString();
    }

    public static final Creator<Review> CREATOR = new Creator<Review>() {
        @Override
        public Review createFromParcel(Parcel in) {
            return new Review(in);
        }

        @Override
        public Review[] newArray(int size) {
            return new Review[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(author);
        dest.writeString(content);
        dest.writeString(url);
    }
}
