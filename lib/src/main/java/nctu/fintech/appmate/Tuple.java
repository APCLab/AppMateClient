package nctu.fintech.appmate;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 值組，即資料表中的一列。
 * <p>
 *     此型別操作方式類似於 {@link JSONObject}，由於資料庫支援部分較特殊之操作，故另建本型別作為預設回傳值。對於程式有相容性需求者，請使用 {@link Tuple#toJSONObject()} 方法轉型。
 * </p>
 * <p>
 *     Class {@link Tuple} represents a tuple on {@code appmate} database.
 *     This class  offers {@link JSONObject}-like API.
 * </p>
 * <p>
 *     For concern of compatible, developer can cast this class to Android {@link JSONObject} by using {@link Tuple#toJSONObject()},
 *     or Google Gson {@link JsonObject} by {@link Tuple#toJsonObject()}.
 * </p>
 */
public class Tuple {

    /*
     * Constants
     */

    private final static String DATE_FORMAT = "yyyy-MM-dd";
    private final static String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private final static int INDEX_TABLE_NAME = 2;
    private final static int INDEX_ITEM_ID = 3;
    private final static String PREFIX_IMG = "IMG";

    /*
     * Global variables
     */

    /**
     * the {@link Table} which owns this {@link Tuple}.
     */
    private TableCore _core;

    /**
     * the container where elements actually saved.
     */
    private JsonObject _obj;

    /**
     * the container saved images
     */
    private Map<String, Bitmap> _img;

    /*
     * Constructors
     */

    /**
     * Create an empty {@link Tuple} instance.
     */
    public Tuple() {
        reset(new NullCore(), new JsonObject());
    }

    /**
     * Create an empty {@link Tuple} instance from retrieved {@link JsonObject}.
     *
     * @param core
     * @param o
     */
    Tuple(TableCore core, JsonObject o) {
        reset(core, o);
    }

    /*
     * Basic operation
     */

    /**
     * Reset tuple by specific data.
     *
     * @param core
     * @param o
     */
    void reset(TableCore core, JsonObject o) {
        _core = core;
        _obj = o;
        _img = new LinkedHashMap<>();
    }

    /*
     * Type conversion
     */

    /**
     * 轉型為{@link JSONObject}。
     * <p>
     * Cast type to {@link JSONObject}.
     * </p>
     *
     * @return a {@link JSONObject} instance
     */
    public JSONObject toJSONObject() {
        try {
            return new JSONObject(_obj.toString());
        } catch (JSONException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * 轉型為{@link JsonObject}。
     * <p>
     * 請注意{@link JsonObject}跟{@link JSONObject}是不一樣的。
     * </p>
     * <p>
     * Cast type to GSON {@link JsonObject}.
     * </p>
     * <p>
     * Notice this it NOT {@link JSONObject}.
     * For usage of {@link JSONObject}, use {@link Tuple#toJSONObject()} instead
     * </p>
     *
     * @return a {@link JsonObject} instance
     */
    public JsonObject toJsonObject() {
        return _obj;
    }

    /**
     * 輸出JSON字串。
     * <p>
     * Get JSON string
     * </p>
     *
     * @return JSON string
     */
    @Override
    public String toString() {
        return _obj.toString();
    }

    /*
     * Properties
     */

    /**
     * 取得容器內資料配對數。
     * <p>
     * Get number of key-value pairs in this container.
     * </p>
     *
     * @return size of this container
     */
    public int size() {
        return _obj.size();
    }

    /**
     * 取得此容器是否為空。
     * <p>
     * Whether the set is empty or not.
     * </p>
     *
     * @return this container is empty or not
     */
    public boolean isEmpty() {
        return _obj.size() == 0;
    }

    /**
     * 取得此物件ID。
     * <p>
     * Get id of this container.
     * </p>
     *
     * @return id, or -1 when this element is local
     * @throws UnsupportedOperationException when item id is not assigned.
     */
    public int getId() {
        if (!_obj.has("id")) {
            throw new UnsupportedOperationException("item id is not assigned.");
        }
        return _obj.get("id").getAsInt();
    }

    /**
     * 取得此容器是否包含某欄位。
     * <p>
     * Get if this container has certain key or not
     * </p>
     *
     * @param key key to search
     * @return whether this container has certain key or not
     */
    public boolean has(String key) {
        return _obj.has(key);
    }

    /**
     * 取得內容集合，此方法適用於 {@code foreach} 陳述。
     * <p>
     * Returns a {@link Set} view of the mappings contained in this container
     * </p>
     *
     * @return a set view of the mappings contained in this container
     */
    @NonNull
    public Set<Map.Entry<String, String>> entrySet() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> p : _obj.entrySet()) {
            JsonElement element = p.getValue();
            if (!(element instanceof JsonPrimitive)) {
                map.put(p.getKey(), element.toString());
            }

            JsonPrimitive primitive = (JsonPrimitive) element;
            if (primitive.isString()) {
                map.put(p.getKey(), primitive.getAsString());
            } else {
                map.put(p.getKey(), primitive.toString());
            }
        }
        return map.entrySet();
    }

    /**
     * Get item url on db.
     *
     * @return url
     * @throws UnsupportedOperationException when item id is not assigned.
     */
    String toUrl() {
        return String.format("%s%s/", _core.url, getId());
    }

    /**
     * Get image to upload.
     *
     * @param key ~
     * @return ~
     */
    Bitmap getUploadBitmap(String key) {
        return (!key.startsWith(PREFIX_IMG) || !_img.containsKey(key)) ? null : _img.get(key); //TODO remove it! 不該這樣判定
    }

    /*
     * `Get` methods
     */

    /**
     * 取得值。
     * <p>
     * Get value as {@link String} type.
     * </p>
     *
     * @param key key
     * @return value
     * @throws ClassCastException if the element not a valid string value.
     */
    public String get(String key) {
        JsonElement element = _obj.get(key);
        if (!(element instanceof JsonPrimitive)) {
            return element.toString();
        }

        JsonPrimitive primitive = (JsonPrimitive) element;
        if (primitive.isString()) {
            return primitive.getAsString();
        } else {
            return primitive.toString();
        }
    }

    /**
     * 取得值，並嘗試轉型為{@code boolean}。
     * <p>
     * Get value as {@code boolean} type.
     * </p>
     *
     * @param key key
     * @return value
     * @throws ClassCastException if the element not a valid boolean value.
     */
    public boolean getAsBoolean(String key) {
        return _obj.get(key).getAsBoolean();
    }

    /**
     * 取得值，並嘗試轉型為{@code byte}。
     * <p>
     * Get value as {@code byte} type.
     * </p>
     *
     * @param key key
     * @return value
     * @throws ClassCastException if the element not a valid byte value.
     */
    public byte getAsByte(String key) {
        return _obj.get(key).getAsByte();
    }

    /**
     * 取得值，並嘗試轉型為{@code char}。
     * <p>
     * Get value as {@code char} type.
     * </p>
     *
     * @param key key
     * @return value
     * @throws ClassCastException if the element not a valid char value.
     */
    public char getAsChar(String key) {
        return _obj.get(key).getAsCharacter();
    }

    /**
     * 取得值，並嘗試轉型為{@code float}。
     * <p>
     * Get value as {@code float} type.
     * </p>
     *
     * @param key key
     * @return value
     * @throws ClassCastException if the element not a valid float value.
     */
    public float getAsFloat(String key) {
        return _obj.get(key).getAsFloat();
    }

    /**
     * 取得值，並嘗試轉型為{@code double}。
     * <p>
     * Get value as {@code double} type.
     * </p>
     *
     * @param key key
     * @return value
     * @throws ClassCastException if the element not a valid double value.
     */
    public double getAsDouble(String key) {
        return _obj.get(key).getAsDouble();
    }

    /**
     * 取得值，並嘗試轉型為{@code int}。
     * <p>
     * Get value as {@code int} type.
     * </p>
     *
     * @param key key
     * @return value
     * @throws ClassCastException if the element not a valid integer value.
     */
    public int getAsInt(String key) {
        return _obj.get(key).getAsInt();
    }

    /**
     * 取得值，並嘗試轉型為日期({@link Date})型別。
     * <p>
     * Get value as {@link Date} type.
     * </p>
     *
     * @param key key
     * @return value
     * @throws ClassCastException if the element not a valid date value.
     */
    public Date getAsDate(String key) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        try {
            return sdf.parse(_obj.get(key).getAsString());
        } catch (ParseException e) {
            throw new ClassCastException(e.getMessage());
        }
    }

    /**
     * 取得值，並嘗試轉型為日期時間({@link Calendar})型別。
     * <p>
     * Get value as {@link Calendar}(aka timestamp) type.
     * </p>
     *
     * @param key key
     * @return value
     * @throws ClassCastException if the element not a valid timestamp value.
     */
    public Calendar getAsCalendar(String key) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault());
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(_obj.get(key).getAsString()));
            return calendar;
        } catch (ParseException e) {
            throw new ClassCastException(e.getMessage());
        }
    }

    /**
     * 取得外來鍵值。
     * 需特別注意此方法會建立連線已取得外來鍵資訊。
     * <p>
     * Get foreign key value as {@link Tuple} type. It should be noticed that this method create a network connection.
     * </p>
     *
     * @param key key
     * @return value
     * @throws ClassCastException            if the element not a valid url value.
     * @throws UnsupportedOperationException if the value is not in the same db as this item.
     * @throws IOException                   table not exist or network error
     * @throws NetworkOnMainThreadException  if this method is called on main thread
     */
    public Tuple getAsTuple(String key) throws IOException {
        URL url = new URL(_obj.get(key).getAsString());

        // check domain
        if (!url.getHost().equals(_core.url.getHost())) {
            throw new UnsupportedOperationException("cross domain query not supported");
        }

        // build table core
        String[] param = url.getPath().split("/");
        String tableNm = param[INDEX_TABLE_NAME];
        int id = Integer.parseInt(param[INDEX_ITEM_ID]);

        Table table = new Table(_core, tableNm);
        return table.get(id);
    }

    /**
     * 取得圖片。
     * 需特別注意此方法會建立連線已取得外來鍵資訊。
     * <p>
     * Get foreign key value as {@link Tuple} type. It should be noticed that this method create a network connection.
     * </p>
     *
     * @param key key
     * @return value
     * @throws ClassCastException            if the element not a valid url value.
     * @throws UnsupportedOperationException if the value is not in the same db as this item.
     * @throws IOException                   table not exist or network error
     * @throws NetworkOnMainThreadException  if this method is called on main thread
     */
    public Bitmap getAsBitmap(String key) throws IOException {
        // local operation
        if (_img.containsKey(key)) {
            return _img.get(key);
        }

        // check domain
        URL url = new URL(_obj.get(key).getAsString());
        if (!url.getHost().equals(_core.url.getHost())) {
            throw new UnsupportedOperationException("cross domain query not supported");
        }

        // create connection
        HttpURLConnection con = _core.openUrl(url);

        // check response code
        int code = con.getResponseCode(); //TODO merge this to DbCore
        if (code != HttpURLConnection.HTTP_OK) {
            Log.e(getClass().getName(), "unexpected HTTP response code received: " + code);
        }

        // read response
        try (InputStream input = con.getInputStream()) {
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            _img.put(key, bitmap); //TODO 建立欄位修改紀錄，簡化上傳量
            return bitmap;
        }
    }

    /*
     * `Put` methods
     */

    /**
     * 新增一組鍵值對到容器中。
     * <p>
     * Add a {@link String} value into this container.
     * </p>
     *
     * @param key   key
     * @param value value
     */
    public void put(String key, String value) {
        _obj.addProperty(key, value);
    }

    /**
     * 新增一組鍵值對到容器中。
     * <p>
     * Add a {@link Number} value into this container.
     * </p>
     *
     * @param key   key
     * @param value value
     */
    public void put(String key, Number value) {
        _obj.addProperty(key, value);
    }

    /**
     * 新增一組鍵值對到容器中。
     * <p>
     * Add a {@link Character} value into this container.
     * </p>
     *
     * @param key   key
     * @param value value
     */
    public void put(String key, Character value) {
        _obj.addProperty(key, value);
    }

    /**
     * 新增一組鍵值對到容器中。
     * <p>
     * Add a {@link Boolean} value into this container.
     * </p>
     *
     * @param key   key
     * @param value value
     */
    public void put(String key, Boolean value) {
        _obj.addProperty(key, value);
    }

    /**
     * 新增一組鍵值對到容器中。
     * <p>
     * Add a {@link Date} value into this container.
     * </p>
     *
     * @param key   key
     * @param value value
     */
    public void put(String key, Date value) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        _obj.addProperty(key, sdf.format(value.getTime()));
    }

    /**
     * 新增一組鍵值對到容器中。
     * <p>
     * Add a {@link Calendar} value into this container.
     * </p>
     *
     * @param key   key
     * @param value value
     */
    public void put(String key, Calendar value) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault());
        _obj.addProperty(key, sdf.format(value.getTime()));
    }

    /**
     * 新增一組鍵值對到容器中。
     * <p>
     * Add a {@link Tuple} value into this container.
     * </p>
     *
     * @param key   key
     * @param value value
     */
    public void put(String key, Tuple value) {
        _obj.addProperty(key, value.toUrl());
    }

    /**
     * 新增一組鍵值對到容器中。
     * <p>
     * Add a {@link Bitmap}(aka image) into this container.
     * </p>
     *
     * @param key   key
     * @param value the image to added
     */
    public void put(String key, Bitmap value) {
        String name = String.format("%s%x%x", PREFIX_IMG, System.currentTimeMillis(), new Random().nextInt());
        _obj.addProperty(key, name);
        _img.put(key, value);
    }

    /*
     * `Remove` method
     */

    /**
     * 自容器中移除一組鍵值對。
     * <p>
     * Remove a key-value pair.
     * </p>
     *
     * @param key key
     * @return success or not
     */
    public boolean remove(String key) {
        return (_obj.remove(key) != null)
                && (_img.remove(key) != null);
    }

}