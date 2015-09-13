package filmster.com.example.com.filmster;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by tgreen on 9/7/15.
 */
public class Clip implements Parcelable {
    public String key;
    public String name;
    public String type;

    public Clip() {
        key = null;
        name = null;
        type = null;
    }

    public Clip(Parcel in){
        String[] theGoods = new String[3];
        in.readStringArray(theGoods);
        key = theGoods[0];
        name = theGoods[1];
        type = theGoods[3];
    }

    public static final Parcelable.Creator<Clip> CREATOR = new Parcelable.Creator<Clip> () {

        @Override
        public Clip createFromParcel(Parcel source) {
            return new Clip(source);
        }

        @Override
        public Clip[] newArray(int size) {
            return new Clip[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String[] mystuff = {key,name,type};
        dest.writeStringArray(mystuff);
    }
}
