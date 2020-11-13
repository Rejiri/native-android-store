package com.lopicard.sonial;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.icu.text.TimeZoneNames;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.DefaultSliderView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okio.BufferedSink;

interface ICommandResult {
    void reply(Object obj);
}

interface IDataReceiver {
    void setData(Object obj);
}

interface IItemHolder {
    Item item = null;

    void setItem(Item item);
}

public class Applica extends Application {
    public static boolean debugMode = true;

    public static Applica Current;
    public static Context Context;
    public JSONObject jSettings;
    public com.lopicard.sonial.Database Database;
    public ChersiManager ChersiManager;
    public Random random;
    public File dataFile;

    public Section CurrentSection;
    public Group CurrentGroup;
    public Item CurrentItem;
    public Address TempAddress;
    public int CurrentTime;
    private int currentAppVersion;

    public Applica() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Applica.Context = this.getApplicationContext();
        Applica.Current = this;
        this.currentAppVersion = 1;
        this.random = new Random();
        Applica.Current.Database = new Database();
        this.ChersiManager = new ChersiManager();
        this.dataFile = new File(this.getCacheDir(), "AF65");
    }

    public Address getUser() {
        ArrayList<Address> aCol = this.Database.getAddresses();
        for (int i = 0; i < aCol.size(); i++)
            if (aCol.get(i).IsRegistered)
                return aCol.get(i);
        Address address = new Address();
        address.Name = "Guest";
        address.Mobile = "(555) 123-4567";
        return address;
    }

    public Calendar getServerTime() throws Exception {
        String dateTime = this.jSettings.getString("dateTime");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyyTHH:mm:ss tt", Locale.US);
        Date date = sdf.parse(dateTime);
        Misc.Log(date.toString());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    public void fetchData(final ICommandResult iCommandResult) {
        OnlineService.postJson(Misc.UrlSonialSettings, "{}", new ICommandResult() {
            @Override
            public void reply(Object obj) {
                Misc.Log("Settings: %s", obj);
                if (obj == null)
                    iCommandResult.reply(false);
                else {
                    try {
                        Applica.Current.jSettings = new JSONObject((String) obj);
                        if (Applica.Current.shouldUpgrade()) {
                            iCommandResult.reply(Misc.MsgShouldUpgrade);
                            return;
                        }
                        if (Applica.Current.ShouldDownloadData()) {
                            Applica.this.Database.fetchData(new ICommandResult() {
                                @Override
                                public void reply(Object obj) {
                                    if ((boolean) obj) {
                                        Applica.this.getSharedPreferences("settings", MODE_PRIVATE).edit().putLong("dataLastUploaded", Applica.Current.dataLastUploaded()).apply();
                                        Applica.Current.Database.loadDataLocally(Applica.Current.dataFile);
                                        iCommandResult.reply(true);
                                    } else
                                        iCommandResult.reply(false);
                                }
                            });
                        } else {
                            Applica.Current.Database.loadDataLocally(Applica.Current.dataFile);
                            iCommandResult.reply(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        iCommandResult.reply(false);
                    }
                }
            }
        });
    }

    private boolean ShouldDownloadData() {
        Misc.Log("ShouldDownloadData, dataLastUploaded_locally: %d, dataLastUploaded_net: %d",
                this.getSharedPreferences("settings", MODE_PRIVATE).getLong("dataLastUploaded", -1),
                Applica.Current.dataLastUploaded());

        if (this.getSharedPreferences("settings", MODE_PRIVATE).getLong("dataLastUploaded", -1) == Applica.Current.dataLastUploaded())
            if (this.dataFile.exists())
                return false;

        Misc.Log("ShouldDownloadData: %b", true);
        return true;
    }

    private boolean shouldUpgrade() {
        if (this.jSettings.optInt("appVersion", 0) > this.currentAppVersion)
            return true;
        return false;
    }

    public int getHHmm() {
        return this.jSettings.optInt("hhmm", 0);
    }

    private long dataLastUploaded() {
        return this.jSettings.optLong("dataLastUploaded", 0);
    }
}

class Database {
    public long dbVersion;
    private ArrayList<Address> addresses;
    private ArrayList<Section> sections;

    public Database() {
    }

    public void fetchData(final ICommandResult iCommandResult) {
        Misc.Log("fetchData");
        OnlineService.GetByteArray(Misc.UrlGetData, Misc.getDeviceInfoJson(), new ICommandResult() {
            @Override
            public void reply(Object obj) {
                if (obj == null)
                    iCommandResult.reply(false);
                else {
                    try {
                        FileOutputStream outputStream = new FileOutputStream(Applica.Current.dataFile);
                        outputStream.write((byte[]) obj);
                        outputStream.flush();
                        outputStream.close();
                        iCommandResult.reply(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        iCommandResult.reply(false);
                    }
                }
            }
        });
    }

    public void loadDataLocally(File file) {
        try {
            Misc.Log("loadDataLocally, length: %d", file.length());
            this.parseFile(new DataInputStream(new FileInputStream(file)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refresh(boolean loadFromDbFile) {
        if (loadFromDbFile)
            try {
                this.readDbFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public ArrayList<Address> getAddresses() {
        if (this.addresses == null) {
            File file = new File(Applica.Current.getCacheDir(), "46AC");
            if (file.exists()) {
                try {
                    String jStr = new BufferedReader(new FileReader(file)).readLine();

                    Misc.Log(jStr);

                    JSONArray jArr = new JSONArray(jStr);
                    JSONObject obj = null;

                    this.addresses = new ArrayList<>();
                    for (int i = 0; i < jArr.length(); i++) {
                        this.addresses.add(Address.fromJSON(jArr.getJSONObject(i)));
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                this.addresses = new ArrayList<>();

//                Address address = new Address();
//                address.Name = "John";
//                address.City = "Seol";
//                address.Mobile = "123";
//                address.Street = "Loias Street No.7";
//                this.addresses.add(address);
            }
        }
        return this.addresses;
    }

    public void saveAddress(Address address) {

        ArrayList<Address> aCol = this.getAddresses();
        address.setId(aCol.size() + 1);
        aCol.add(address);

        try {
            JSONArray jArr = new JSONArray();
            for (int i = 0; i < aCol.size(); i++) {
                jArr.put(i, aCol.get(i).toJSON());
            }

            File file = new File(Applica.Current.getCacheDir(), "46AC");
            FileOutputStream foStream = new FileOutputStream(file, false);
            foStream.write(jArr.toString().getBytes());
            foStream.flush();
            foStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Section> getSections() {
        if (this.sections == null) {
            this.sections = new ArrayList<>();
            sections.add(new Section(0, "Supermarkets"));
            sections.add(new Section(1, "Restaurants"));
            sections.add(new Section(2, "Library"));
            sections.add(new Section(3, "Grocery"));
            sections.add(new Section(4, "Pharmacy"));
            sections.add(new Section(5, "Misc"));
        }
        return this.sections;
    }

    public ArrayList<Group> getGroupsOf(Section section) {
        ArrayList<Group> groups = new ArrayList<>();

        int count = new Random().nextInt(10) + 1;
        for (int i = 0; i < count; i++) {
            groups.add(new Group(section, i, "Group #" + i + 1));
        }

        return groups;
    }

    public ArrayList<Item> getItemsOf(Group group) {
        ArrayList<Item> items = new ArrayList<>();

        int count = new Random().nextInt(10) + 1;
        for (int i = 0; i < count; i++) {
            items.add(new Item(group, i, "Item " + i + 1));
        }

        return items;
    }

    public void readDbFile() throws IOException {
        InputStream inputStream = null;
        DataInputStream dStream = null;

        try {
            inputStream = Applica.Current.getAssets().open("Sonial.dat");
            dStream = new DataInputStream(inputStream);
            this.parseFile(dStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!(dStream == null))
                dStream.close();
            if (!(inputStream == null))
                inputStream.close();
        }
    }

    public void parseFile(DataInputStream dStream) throws IOException {
        this.dbVersion = ByteSwapper.swap(dStream.readLong());
        long lastId = ByteSwapper.swap(dStream.readLong());

        Section section = null;
        this.sections = new ArrayList<>();
        int sectionCount = ByteSwapper.swap(dStream.readInt());
        for (int i = 0; i < sectionCount; i++) {
            section = Section.deserializeSection(dStream);
            if (section.Available)
                this.sections.add(section);
        }
    }

    public ArrayList<Item> searchFor(String text) {
        Section section = null;
        Group group = null;
        Item item = null;

        ArrayList<Item> items = new ArrayList<>();

        // for (int i = 0; i < this.getSections().size(); i++) {
        // section = this.getSections().get(i);
        section = Applica.Current.CurrentSection;
        for (int j = 0; j < section.getGroups().size(); j++) {
            group = section.getGroups().get(j);
            for (int k = 0; k < group.getItems().size(); k++) {
                item = group.getItems().get(k);

                if (item.Name.contains(text))
                    items.add(item);

                if (items.size() >= 8)
                    return items;
            }
        }

        return items;
    }

    public Item getItemById(long id) {
        Section section = null;
        Group group = null;
        Item item = null;

        for (int i = 0; i < this.getSections().size(); i++) {
            section = this.getSections().get(i);
            for (int j = 0; j < section.getGroups().size(); j++) {
                group = section.getGroups().get(j);
                for (int k = 0; k < group.getItems().size(); k++) {
                    item = group.getItems().get(k);
                    if (item.Id == id)
                        return item;
                }
            }
        }
        return null;
    }
}

class OnlineService {
    public static void GetByteArray(final String url, final JSONObject jsonObject, final ICommandResult iCommandResult) {
        AsyncTask asyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return MediaType.parse("text/x-markdown; charset=utf-8");
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        sink.writeUtf8(jsonObject.toString());
                    }
                };

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url).post(requestBody).build();

                try {
                    okhttp3.Response response = client.newCall(request).execute();
                    if (response.isSuccessful())
                        return response.body().bytes();
                    return null;
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Object o) {
                try {
                    iCommandResult.reply(o);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        asyncTask.execute();
    }

    public static void postJson(final String url, final String jsonString, final ICommandResult iCommandResult) {
        AsyncTask asyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    return new OkHttpClient().newCall(
                            new okhttp3.Request.Builder().url(url).post(
                                    RequestBody.create(MediaType.parse(Misc.MediaTypeJson), jsonString))
                                    .build())
                            .execute().body().string();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Object o) {
                try {
                    iCommandResult.reply(o);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        asyncTask.execute();
    }
}

class Section {
    public int Id;
    public String Name;
    public boolean Available;

    private ArrayList<Group> groups;

    public Section(int id, String name) {
        this.Id = id;
        this.Name = name;
    }

    public ArrayList<Group> getGroups() {
        if (this.groups == null)
            this.groups = Applica.Current.Database.getGroupsOf(this);
        return this.groups;
    }

    public static ArrayList<Section> getSections() {
        return Applica.Current.Database.getSections();
    }

    public static Section deserializeSection(DataInputStream dStream) throws IOException {
        if (Applica.Current.Database.dbVersion < 3) {
            Section section = new Section(ByteSwapper.swap(dStream.readInt()), Misc.ReadUTF8(dStream));

            section.groups = new ArrayList<>();
            int groupCount = ByteSwapper.swap(dStream.readInt());
            for (int i = 0; i < groupCount; i++)
                section.groups.add(Group.deserializeGroup(dStream, section));
            return section;
        } else {
            Section section = new Section(ByteSwapper.swap(dStream.readInt()), Misc.ReadUTF8(dStream));

            section.Available = dStream.readBoolean();
            section.groups = new ArrayList<>();
            int groupCount = ByteSwapper.swap(dStream.readInt());
            for (int i = 0; i < groupCount; i++)
                section.groups.add(Group.deserializeGroup(dStream, section));
            return section;
        }
    }

    public int getPhotoResId() {
        return Misc.getIdFromResource(Misc.format("section_%d", this.Id + 1));
    }

    public Uri getPhotoUri() {
        return Uri.parse(Misc.format(Misc.UrlSection, this.Id + 1));
    }

    public int getIconResId() {
        return Misc.getIdFromResource(Misc.format("section_icon_%d", this.Id + 1));
    }

    public int itemsCount() {
        int re = 0;
        for (int i = 0; i < this.getGroups().size(); i++)
            re += this.getGroups().get(i).itemsCount();
        return re;
    }

    public Item getItemById(long id) {
        for (int i = 0; i < this.groups.size(); i++)
            return this.groups.get(i).getItemById(id);
        return null;
    }
}

class Group {
    public long Id;
    public Section SectionIn;
    public String Name;
    public String Description;
    public int OpenTime;
    public int CloseTime;

    private ArrayList<Item> items;

    public Group(Section section, long id, String name) {
        this.SectionIn = section;
        this.Id = id;
        this.Name = name;
    }

    public ArrayList<Item> getItems() {
        if (this.items == null)
            this.items = Applica.Current.Database.getItemsOf(this);
        return this.items;
    }

    public static Group deserializeGroup(DataInputStream dStream, Section section) throws IOException {
        Group group = new Group(section, ByteSwapper.swap(dStream.readLong()), Misc.ReadUTF8(dStream));
        group.Description = Misc.ReadUTF8(dStream);
        group.OpenTime = ByteSwapper.swap(dStream.readInt());
        group.CloseTime = ByteSwapper.swap(dStream.readInt());

        group.items = new ArrayList<>();
        int itemCount = ByteSwapper.swap(dStream.readInt());
        for (int i = 0; i < itemCount; i++)
            group.items.add(Item.deserializeItem(dStream, group));
        return group;
    }

    public int itemsCount() {
        return this.getItems().size();
    }

    public Item getItemById(long id) {
        for (int i = 0; i < this.items.size(); i++) {
            Misc.Log("ItemId: %d - SearchFor: %d, Equals: %b", this.items.get(i).Id, id, this.items.get(i).Id == id);
            if (this.items.get(i).Id == id)
                return this.items.get(i);
        }
        return null;
    }

    public boolean withinAvailableTime() {
        Misc.Log("HHmm: %d - OpenTime: %d - CloseTime: %d", Applica.Current.getHHmm(), this.OpenTime, this.CloseTime);

        if (this.OpenTime == 0 && this.CloseTime == 0)
            return true;

        if (Applica.Current.getHHmm() >= this.OpenTime &&
                Applica.Current.getHHmm() < this.CloseTime)
            return true;
        return false;
    }
}

class Item {
    public long Id;
    public Group GroupIn;
    public String Name;
    public String Description;
    public int Quantity;
    public float Price;
    public int DeliveryDays;
    public float DeliveryFees;
    public float Weight;
    public float Capacity;
    public boolean Available;
    public int LikeCount;
    public int RequestsCount;
    public ArrayList<String> photos;

    public Item(Group group, long id, String name) {
        this.GroupIn = group;
        this.Id = id;
        this.Name = name;
        this.LikeCount = Applica.Current.random.nextInt(100);
        this.RequestsCount = Applica.Current.random.nextInt(100);
    }

    public String getDefaultPhotoPath() {
        if (this.photos.size() > 0)
            return Misc.format("http://akrogroup.net/Sonial/%d/%s", this.Id, this.photos.get(0));
        return "http://contractor911.com/wp-content/themes/ultra-white/assets/images/placeholder.jpg";
    }

    public boolean isLiquid() {
        return this.Capacity > 0;
    }

    public String getMassText() {
        if (this.isLiquid())
            return Loca.getValue(Loca.VCapacity);
        return Loca.getValue(Loca.VWeight);
    }

    public float getMass() {
        if (this.isLiquid())
            return this.Capacity;
        return this.Weight;
    }

    public String getMassFormatted() {
        if (this.isLiquid())
            return Misc.format("%.0f ml", this.Capacity);
        return Misc.format("%.0f g", this.Weight);
    }

    public static Item getItemById(long id) {
        return Applica.Current.Database.getItemById(id);
    }

    public static Item deserializeItem(DataInputStream dStream, Group group) throws IOException {
        Item item = new Item(group, ByteSwapper.swap(dStream.readLong()), Misc.ReadUTF8(dStream));
        item.Description = Misc.ReadUTF8(dStream);
        item.Quantity = ByteSwapper.swap(dStream.readInt());
        item.Price = ByteSwapper.swap(dStream.readFloat());
        item.DeliveryDays = ByteSwapper.swap(dStream.readInt());
        item.DeliveryFees = ByteSwapper.swap(dStream.readFloat());
        item.Weight = ByteSwapper.swap(dStream.readFloat());
        item.Capacity = ByteSwapper.swap(dStream.readFloat());
        item.Available = dStream.readBoolean();

        item.photos = new ArrayList<>();
        int photosCount = ByteSwapper.swap(dStream.readInt());
        for (int i = 0; i < photosCount; i++)
            item.photos.add(Misc.ReadUTF8(dStream));
        return item;
    }
}

class ChersiManager {
    public Chersi CurrentChersi;
    // public ArrayList<Chersi> chersis;

    public ChersiManager() {
        // this.chersis = new ArrayList<>();
        this.newChersi();
    }

    public void newChersi() {
//        if (!(this.CurrentChersi == null))
//            this.chersis.add(this.CurrentChersi);
        this.CurrentChersi = new Chersi();
    }

    public ArrayList<Chersi> getHistory() {
        // return this.chersis;

        ArrayList<Chersi> cArr = new ArrayList<>();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(Applica.Current.getCacheDir(), "LT64")));
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) break;
                cArr.add(Chersi.fromJson(new JSONObject(line)));
            }
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cArr;
    }

    public void saveToHistory(Chersi chersi) {
        try {
            File file = new File(Applica.Current.getCacheDir(), "LT64");
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            bufferedWriter.write(chersi.toJson().toString());
            bufferedWriter.newLine();
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addIf(ChersiItem chersiItem, boolean addition) {
        if (!chersiItem.item.Available) {
            Toast.makeText(Applica.Context, "sorry, this item is currently unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!chersiItem.item.GroupIn.withinAvailableTime()) {
            Toast.makeText(Applica.Context, "Sorry, this item is currently unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (addition) chersiItem.quantity++;
        else chersiItem.quantity--;

        if (chersiItem.quantity <= 0)
            chersiItem.quantity = 0;

        if (this.CurrentChersi.items.size() == 0)
            this.CurrentChersi.dateTime = Calendar.getInstance();

        boolean exist = this.CurrentChersi.items.contains(chersiItem);
        if (chersiItem.quantity == 0) {
            if (exist)
                this.CurrentChersi.items.remove(chersiItem);
        } else {
            if (!exist)
                this.CurrentChersi.items.add(chersiItem);
        }
    }
}

class Chersi {
    public long id;
    public Calendar dateTime;
    public float discount;
    public float deliveryFees;
    public ArrayList<ChersiItem> items;
    public Address address;
    public String deliveryNote;

    public Chersi() {
        this.dateTime = Calendar.getInstance();
        this.discount = 0;
        this.deliveryFees = 3;
        this.items = new ArrayList<>();
    }

    public float getItemsPrice() {
        float price = 0;
        for (int i = 0; i < this.items.size(); i++)
            price += this.items.get(i).item.Price * this.items.get(i).quantity;
        return price;
    }

    public float getTotalPrice() {
        float price = 0;
        for (int i = 0; i < this.items.size(); i++)
            price += this.items.get(i).item.Price * this.items.get(i).quantity;
        return price - this.discount + this.deliveryFees;
    }

    public int getTotalItemsCount() {
        int count = 0;
        for (int i = 0; i < this.items.size(); i++)
            count += this.items.get(i).quantity;
        return count;
    }

    public ChersiItem getItemIfExist(Item item) {
        for (int i = 0; i < this.items.size(); i++)
            if (this.items.get(i).item.Id == item.Id)
                return this.items.get(i);
        return null;
    }

    public void send(final Context context, final ICommandResult iCommandResult) throws Exception {
        if (this.items.size() == 0) {
            iCommandResult.reply(null);
        } else {
            OnlineService.postJson(Misc.UrlOrderSave, Chersi.this.toJson().toString(), new ICommandResult() {
                @Override
                public void reply(Object obj) {
                    if (obj == null) {
                        iCommandResult.reply(null);
                    } else {
                        try {
                            JSONObject jsonObject = new JSONObject((String) obj);
                            Chersi.this.id = jsonObject.optLong("id");
                            Chersi.this.saveLocally();
                            Applica.Current.ChersiManager.newChersi();
                            iCommandResult.reply(jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            iCommandResult.reply(null);
                        }
                    }
                }
            });
        }

//        JsonObjectRequest request = new JsonObjectRequest(
//                Request.Method.POST,
//                Misc.UrlOrderSave,
//                this.toJson(),
//                new Response.Listener<JSONObject>() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        Misc.Log("Response: %s", response.toString());
//                        Chersi.this.id = response.optLong("id");
//                        // Chersi.this.save();
//                        Applica.Current.ChersiManager.newChersi();
//                        iCommandResult.reply(response);
//                    }
//                },
//                new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        // Misc.Log("Error: %s", new String(error.getMessage()));
//                        error.printStackTrace();
//                        iCommandResult.reply(null);
//                    }
//                }) {
//            @Override
//            protected Map<String, String> getParams() throws AuthFailureError {
//                return super.getParams();
//                //                Map<String, String> params = new HashMap<>();
////                try {
////                    params.put("Order", Chersi.this.toJson().toString());
////                } catch (JSONException e) {
////                    e.printStackTrace();
////                }
////                return params;
//            }
//        };
//        Volley.newRequestQueue(Applica.Context).add(request);
    }

    private void saveLocally() {
        Applica.Current.ChersiManager.saveToHistory(this);
    }

    public JSONObject toJson() throws JSONException {
        JSONArray jArr = new JSONArray();
        for (int i = 0; i < this.items.size(); i++)
            jArr.put(i, this.items.get(i).toJSON());

        JSONObject jObj = new JSONObject();
        jObj.put("id", this.id);
        jObj.put("dateTime", new SimpleDateFormat("ddMMyyyyHHmm").format(this.dateTime.getTime()));
        jObj.put("address", this.address.toJSON());
        jObj.put("deliveryNote", this.deliveryNote);
        jObj.put("items", jArr);
        Misc.Log(jObj.toString());
        return jObj;
    }

    public static Chersi fromJson(JSONObject jsonObject) throws JSONException, ParseException {
        Chersi chersi = new Chersi();
        chersi.id = jsonObject.getLong("id");
        chersi.dateTime = Calendar.getInstance();
        chersi.dateTime.setTime(new SimpleDateFormat("ddMMyyyyHHmm").parse(jsonObject.getString("dateTime")));
        chersi.address = Address.fromJSON(jsonObject.getJSONObject("address"));
        chersi.deliveryNote = jsonObject.optString("deliveryNote");

        JSONArray jArr = jsonObject.getJSONArray("items");
        for (int i = 0; i < jArr.length(); i++)
            chersi.items.add(ChersiItem.fromJson(jArr.getJSONObject(i)));

        return chersi;
    }
}

class ChersiItem {
    public Item item;
    public int quantity;
    public String notes;

    public ChersiItem(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jObj = new JSONObject();
        jObj.put("itemId", this.item.Id);
        jObj.put("quantity", this.quantity);
        jObj.put("notes", this.notes);
        return jObj;
    }

    public static ChersiItem fromJson(JSONObject jsonObject) throws JSONException {
        long no = jsonObject.getLong("itemId");
        ChersiItem chersiItem = new ChersiItem(Applica.Current.Database.getItemById(no), jsonObject.getInt("quantity"));
        chersiItem.notes = jsonObject.optString("notes");
        return chersiItem;
    }
}

class Address {
    public long Id;
    public String Name;
    public String Email;
    public String City;
    public String Phone;
    public String Mobile;
    public String Street;
    public boolean IsDefault;
    public boolean IsRegistered;
    public String Photo;

    public Address() {
    }

    public JSONObject toJSON() {
        JSONObject jObj = new JSONObject();
        try {
            jObj.put("id", this.Id);
            jObj.put("name", this.Name);
            jObj.put("email", this.Email);
            jObj.put("city", this.City);
            jObj.put("phoneNo", this.Phone);
            jObj.put("mobileNo", this.Mobile);
            jObj.put("address", this.Street);
            jObj.put("isDefault", this.IsDefault);
            jObj.put("isRegistered", this.IsRegistered);
            jObj.put("photo", this.Photo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jObj;
    }

    public void setId(long id) {
        this.Id = id;
    }

    public static Address fromJSON(JSONObject jsonObject) {
        Address address = new Address();
        address.setId(jsonObject.optLong("id"));
        address.Name = jsonObject.optString("name");
        address.Email = jsonObject.optString("email");
        address.City = jsonObject.optString("city");
        address.Phone = jsonObject.optString("phoneNo");
        address.Mobile = jsonObject.optString("mobileNo");
        address.Street = jsonObject.optString("address");
        address.IsDefault = jsonObject.optBoolean("isDefault", false);
        address.IsRegistered = jsonObject.optBoolean("isRegistered", false);
        address.Photo = jsonObject.optString("photo");
        return address;
    }
}

class Prescription {
    public String text;

    public Prescription(String text) {
        this.text = text;
    }

    public void send(final ICommandResult onDone) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                Misc.UrlSavePrescription,
                this.toJson(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onDone.reply(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onDone.reply(error.getMessage());
                    }
                });

        Volley.newRequestQueue(Applica.Context).add(request);
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("text", this.text);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}

class Misc {
    public static long timeDiff = 621355968000000000l;
    public static String ColorGreen = "#00b84c";
    public static String ColorRed = "#ec1b30";
    public static String MediaTypeJson = "application/json; charset=utf-8";
    public static String SectionPhotoPath = "http://akrogroup.net/Sonial/Section";
    public static String UrlOrderSave = "http://sonial.akrogroup.net/Home/SaveOrder";
    public static String UrlSavePrescription = "http://sonial.akrogroup.net/Home/SavePrescription";
    public static String UrlGetData = "http://sonial.akrogroup.net/Home/GetSonialData";
    public static String MsgCheckNetwork = "check your internet connection and try again.";
    public static String UrlSonialSettings = "http://sonial.akrogroup.net/Home/GetSonialSettings";
    public static String UrlSlide = "http://sonial.akrogroup.net/photos/slide_%d.png";
    public static String UrlSection = "http://sonial.akrogroup.net/photos/section_%d.png";
    public static String MsgSelectOneAtLeast = "يرجى اختيار مادة واحدة على الأقل";
    public static String MsgSelectDeliveryAddress = "يرجى تحديد عنوان التسليم او اضافة عنوان جديد";
    public static String MsgPleaseTryAgain = "تعذر ارسال الطلبية، يرجى اعادة المحاولة";
    public static String MsgEnterRequiredData = "يرجى ادخال جميع البيانات المطلوبة";
    public static String MsgShouldUpgrade = "يرجى ترقية التطبيق الى الاصدار الجديد";
    public static String MsgError = "Sorry, Something went wrong, please try again later.";
    public static String MsgPrescriptionHasBeenSent = "تم ارسال الوصفة بنجاح";

    public static String ReadUTF8(DataInputStream dStream) {
        try {
            int len = ByteSwapper.swap(dStream.readInt());

            if (len == 0)
                return null;
            if (len < -1)
                return null;

            byte[] utf8Bytes = new byte[len];
            dStream.read(utf8Bytes);

            String text = null;
            try {
                text = new String(utf8Bytes, "UTF-8");
            } catch (Exception e) {
                text = e.getMessage();
            }
            return text;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int unsignedIntFromByteArray(byte[] bytes) {
        int res = 0;
        if (bytes == null)
            return res;

        for (int i = 0; i < bytes.length; i++) {
            res = (res * 10) + ((bytes[i] & 0xff));
        }
        return res;
    }

    public static int getIdFromResource(String format) {
        return Applica.Current.getResources().getIdentifier(
                format, "drawable", Applica.Current.getPackageName());
    }

    public static void Log(String format, Object... args) {
        Log.w("*********: ", Misc.format(format, args));
    }

    public static String format(String format, Object... args) {
        if (format == null)
            format = "NULL";
        return String.format(Locale.US, format, args);
    }

    public static void startActivity(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);

//        if (cls == MainActivity.class)
//            ((Activity) context).finish();
    }

    public static Calendar getCalendar(long millis) {
        return Misc.getCalendar(millis, false);
    }

    public static Calendar getCalendar(long millis, boolean withoutTimeDiff) {
        Calendar calendar = Calendar.getInstance();

        if (withoutTimeDiff)
            calendar.setTimeInMillis(millis - Misc.timeDiff);
        else
            calendar.setTimeInMillis(millis);

        // Misc.Log("mainMilli: %d - afterMilli: %d", millis, millis - Misc.timeDiff);
        return calendar;
    }

    public static String getFormattedDateTime(Calendar calendar, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(calendar.getTime());
    }

    public static String formatPrice(float price, boolean withZeros, boolean withCurrency) {
        if (withZeros)
            return Misc.format("%.2f%s", price, ((withCurrency) ? " " + Loca.getValue(Loca.VPriceCurrency) : ""));
        else
            return Misc.format("%.0f%s", price, ((withCurrency) ? " " + Loca.getValue(Loca.VPriceCurrency) : ""));
    }

    public static void setTextWithColor(View view, String text, String color) {
        ((TextView) view).setTextColor(Color.parseColor(color));
        ((TextView) view).setText(text);
    }

    public static boolean isNullOrEmpty(String text) {
        return text == null || text.length() == 0;
    }

    public static String getStringFromRes(int resId) {
        return Applica.Current.getString(resId);
    }

    public static JSONObject getDeviceInfoJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("BOARD", Build.BOARD);
            jsonObject.put("BOOTLOADER", Build.BOOTLOADER);
            jsonObject.put("BRAND", Build.BRAND);
            jsonObject.put("DEVICE", Build.DEVICE);
            jsonObject.put("DISPLAY", Build.DISPLAY);
            jsonObject.put("FINGERPRINT", Build.FINGERPRINT);
            jsonObject.put("HARDWARE", Build.HARDWARE);
            jsonObject.put("HOST", Build.HOST);
            jsonObject.put("ID", Build.ID);
            jsonObject.put("MANUFACTURER", Build.MANUFACTURER);
            jsonObject.put("MODEL", Build.MODEL);
            jsonObject.put("PRODUCT", Build.PRODUCT);
            jsonObject.put("SERIAL", Build.SERIAL);
            jsonObject.put("TAGS", Build.TAGS);
            jsonObject.put("TYPE", Build.TYPE);
            jsonObject.put("UNKNOWN", Build.UNKNOWN);
            jsonObject.put("USER", Build.USER);

            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getHHmm() {
        Calendar c = Calendar.getInstance();
        int hhMM = ((c.get(Calendar.HOUR_OF_DAY) * 100) + c.get(Calendar.MINUTE));
        Misc.Log("HHmm: %d", hhMM);
        return hhMM;
    }
}

class Loca {
    public static String VPriceCurrency = "VPriceCurrency";
    public static String VCapacity = "VCapacity";
    public static String VWeight = "VWeight";
    public static String VAvailable = "VAvailable";
    public static String VUnAvailable = "VUnAvailable";

    public static String getValue(String key) {
        if (key.equals(Loca.VPriceCurrency))
            return "JD";
        else if (key.equals(Loca.VCapacity))
            return "السعة";
        else if (key.equals(Loca.VWeight))
            return "الوزن";
        else if (key.equals(Loca.VAvailable))
            return "متوفر";
        else if (key.equals(Loca.VUnAvailable))
            return "غير متوفر";
        return "";
    }
}

class SectionAdapter extends RecyclerView.Adapter {
    private ArrayList<Section> sections;
    private View.OnClickListener listener;

    public static class HeaderHolder extends RecyclerView.ViewHolder {
        private SliderLayout mainSlider;

        public HeaderHolder(View itemView) {
            super(itemView);
            this.mainSlider = (SliderLayout) itemView.findViewById(R.id.mainSlider);
        }

        public void setData() {
            this.mainSlider.removeAllSliders();

            int slidesCount = Applica.Current.jSettings.optInt("slidesCount", 1);
            for (int i = 0; i < slidesCount; i++) {
                this.mainSlider.addSlider(
                        new DefaultSliderView(this.mainSlider.getContext())
                                .image(Misc.format(Misc.UrlSlide, i + 1))
                                .setScaleType(BaseSliderView.ScaleType.CenterCrop));
            }
        }
    }

    public static class SectionHolder extends RecyclerView.ViewHolder {
        private Section section;
        private TextView txtName;
        private TextView txtCount;
        private ImageView imgSection;
        private ImageView imgIcon;

        public SectionHolder(View itemView) {
            super(itemView);

            this.txtName = (TextView) itemView.findViewById(R.id.txtName);
            this.txtCount = (TextView) itemView.findViewById(R.id.txtCount);
            this.imgSection = (ImageView) itemView.findViewById(R.id.imgSection);
            this.imgIcon = (ImageView) itemView.findViewById(R.id.imgIcon);
        }

        public void setData(Section section) {
            Misc.Log("setData - Section: %d", section.Id);

            this.section = section;
            this.txtName.setText(this.section.Name);
            this.txtCount.setText(String.valueOf(this.section.itemsCount()));

            Glide.with(this.itemView.getContext()).load(this.section.getPhotoUri()).into(this.imgSection);
            Glide.with(this.itemView.getContext()).load(this.section.getIconResId()).into(this.imgIcon);

//            if (this.section.Id % 2 == 0)
//                this.itemView.setPadding(0, 0, 3, 0);
        }
    }

    public SectionAdapter(final ArrayList<Section> sections) {
        if (sections == null)
            return;

        this.sections = sections;
        this.listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionHolder holder = (SectionHolder) view.getTag();
                Applica.Current.CurrentSection = holder.section;

                if (Applica.Current.CurrentSection.Id == 14)
                    Misc.startActivity(holder.itemView.getContext(), PharmacyActivity.class);
                else
                    Misc.startActivity(holder.itemView.getContext(), SectionActivity.class);
            }
        };
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? 0 : 1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0)
            return new HeaderHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_main_header, parent, false));
        else if (viewType == 1) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_section, parent, false);
            SectionHolder holder = new SectionHolder(view);
            view.setTag(holder);
            view.setOnClickListener(this.listener);
            return holder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == 0)
            ((HeaderHolder) holder).setData();
        else {
            position -= 1;
            ((SectionHolder) holder).setData(this.sections.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return this.sections.size() + 1;
    }
}

class GroupAdapter extends RecyclerView.Adapter {
    private Context context;
    private boolean textOnlyGroups;
    private View viewAll;

    private ArrayList<Group> groups;
    private View.OnClickListener listener;
    private View.OnClickListener moreListener;

    private static Group selectedGroup;

    public static class TextOnlyGroupHolder extends RecyclerView.ViewHolder implements IDataReceiver {
        private Group group;
        private TextView txtName;

        public TextOnlyGroupHolder(View itemView, View.OnClickListener listener) {
            super(itemView);
            itemView.setTag(this);
            itemView.setOnClickListener(listener);

            this.txtName = (TextView) itemView.findViewById(R.id.txtName);
        }

        @Override
        public void setData(Object obj) {
            this.group = (Group) obj;

            this.itemView.setBackgroundResource(0);
            if (this.group == GroupAdapter.selectedGroup)
                // this.itemView.setBackgroundColor(Color.parseColor(Misc.ColorRed));
                this.itemView.setBackgroundResource(R.drawable.round2);

            this.txtName.setText(this.group.Name);
        }
    }

    public static class GroupHolder extends RecyclerView.ViewHolder implements IDataReceiver {
        private Group group;
        private TextView txtName;
        private RecyclerView itemRecycler;

        public GroupHolder(View itemView, View.OnClickListener moreListener) {
            super(itemView);

            this.txtName = (TextView) itemView.findViewById(R.id.txtName);
            this.itemRecycler = (RecyclerView) itemView.findViewById(R.id.itemRecycler);
            this.itemRecycler.setHasFixedSize(true);
            //this.itemRecycler.setLayoutManager(new GridLayoutManager(itemView.getContext(), 1, LinearLayoutManager.HORIZONTAL, false));
            this.itemRecycler.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));

            itemView.findViewById(R.id.txtMore).setTag(this);
            itemView.findViewById(R.id.txtMore).setOnClickListener(moreListener);
        }

        @Override
        public void setData(Object obj) {
            this.group = (Group) obj;
            this.txtName.setText(this.group.Name);
//            this.itemRecycler.setAdapter(new ItemAdapter(
            //                    new ArrayList<Item>(this.group.getItems().subList(0, Math.min(10, this.group.getItems().size()))),
//                    // this.group.getItems(),
//                    true));
            RecyclerView.Adapter adapter = this.itemRecycler.getAdapter();
            if (adapter == null)
                this.itemRecycler.setAdapter(new ItemAdapter(
                        new ArrayList<Item>(this.group.getItems().subList(0, Math.min(10, this.group.getItems().size()))),
                        // this.group.getItems(),
                        true));
            else
                ((ItemAdapter) this.itemRecycler.getAdapter()).setItems(
                        new ArrayList<Item>(this.group.getItems().subList(0, Math.min(10, this.group.getItems().size())))
                        // this.group.getItems()
                );
        }
    }

    public GroupAdapter(Context context, boolean textOnlyGroups, ArrayList<Group> groups, View viewAll) {
        this.context = context;
        this.groups = groups;

        this.textOnlyGroups = textOnlyGroups;
        this.viewAll = viewAll;

        GroupAdapter.selectedGroup = null;

        if (this.textOnlyGroups)
            this.listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TextOnlyGroupHolder holder = (TextOnlyGroupHolder) view.getTag();
                    GroupAdapter.this.setSelectedGroup(holder.group);

                    ((ICommandResult) GroupAdapter.this.context).reply(holder.group);
                }
            };
        else {
            this.moreListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    GroupHolder holder = (GroupHolder) view.getTag();
                    ((ICommandResult) GroupAdapter.this.context).reply(holder.group);
                }
            };
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (this.textOnlyGroups) {
            return new TextOnlyGroupHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_text_only_group, parent, false),
                    this.listener);
        } else {
            return new GroupHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.card_group, parent, false),
                    this.moreListener);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((IDataReceiver) holder).setData(this.groups.get(position));
    }

    @Override
    public int getItemCount() {
        return this.groups.size();
    }

    public void setSelectedGroup(Group group) {
        if (group == null)
            // this.viewAll.setBackgroundColor(Color.parseColor(Misc.ColorRed));
            this.viewAll.setBackgroundResource(R.drawable.round2);
        else
            this.viewAll.setBackgroundResource(0);

        this.selectedGroup = group;
        this.notifyDataSetChanged();
    }
}

class ItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<Item> items;
    private boolean compact;
    private View.OnClickListener listener;

    public static class ItemHolder extends RecyclerView.ViewHolder implements IItemHolder {
        private Item item;
        private TextView txtName;
        private TextView txtPrice;
        private TextView txtLikes;
        private TextView txtGroup;
        private TextView lblWeight;
        private TextView txtCapacity;
        private TextView txtRequestsCount;
        private TextView txtStatus;
        private TextView txtAvailable;
        private View layAvailable;
        private ImageView imgItem;

        public ItemHolder(View itemView) {
            super(itemView);
            this.txtName = (TextView) itemView.findViewById(R.id.txtName);
            this.txtPrice = (TextView) itemView.findViewById(R.id.txtPrice);
            this.txtLikes = (TextView) itemView.findViewById(R.id.txtLikes);
            this.txtGroup = (TextView) itemView.findViewById(R.id.txtGroup);
            this.lblWeight = (TextView) itemView.findViewById(R.id.lblWeight);
            this.txtCapacity = (TextView) itemView.findViewById(R.id.txtCapacity);
            this.txtRequestsCount = (TextView) itemView.findViewById(R.id.txtRequestsCount);
            this.txtStatus = (TextView) itemView.findViewById(R.id.txtStatus);
            this.txtAvailable = (TextView) itemView.findViewById(R.id.txtAvailable);
            this.layAvailable = itemView.findViewById(R.id.layAvailable);
            this.imgItem = (ImageView) itemView.findViewById(R.id.imgItem);
        }

        @Override
        public void setItem(Item item) {
            this.item = item;
            this.txtName.setText(this.item.Name);
            this.txtPrice.setText(Misc.formatPrice(this.item.Price, true, true));
            this.txtLikes.setText(String.valueOf(this.item.LikeCount));
            // this.txtGroup.setText(this.item.GroupIn.Name);
            this.lblWeight.setText(this.item.getMassText());
            this.txtCapacity.setText(this.item.getMassFormatted());
            this.txtRequestsCount.setText(Misc.format("+%d", this.item.RequestsCount));
            Misc.setTextWithColor(
                    this.txtStatus,
                    ((this.item.Available) ? Loca.getValue(Loca.VAvailable) : Loca.getValue(Loca.VUnAvailable)),
                    ((this.item.Available) ? Misc.ColorGreen : Misc.ColorRed));
            Misc.Log(this.item.getDefaultPhotoPath());

            if (this.item.GroupIn.withinAvailableTime()) {
                this.txtAvailable.setText(R.string.TVAvailable);
                this.layAvailable.setBackgroundResource(R.drawable.roundgreen);
            } else {
                this.txtAvailable.setText(R.string.TVUnAvailable);
                this.layAvailable.setBackgroundResource(R.drawable.round);
            }

            // Picasso.with(this.itemView.getContext()).load(this.item.getDefaultPhotoPath()).into(this.imgItem);
            Glide.with(this.itemView.getContext()).load(this.item.getDefaultPhotoPath()).into(this.imgItem);
        }
    }

    public static class ItemHolderCompact extends RecyclerView.ViewHolder implements IItemHolder {
        private Item item;
        private TextView txtName;
        private TextView txtPrice;
        private TextView txtLikes;
        private ImageView imgItem;
        private ImageView imgStatus;

        public ItemHolderCompact(View itemView) {
            super(itemView);
            this.imgItem = (ImageView) itemView.findViewById(R.id.imgItem);
            this.txtName = (TextView) itemView.findViewById(R.id.txtName);
            this.txtPrice = (TextView) itemView.findViewById(R.id.txtPrice);
            this.txtLikes = (TextView) itemView.findViewById(R.id.txtLikes);
            this.imgStatus = (ImageView) itemView.findViewById(R.id.imgStatus);
        }

        @Override
        public void setItem(Item item) {
            this.item = item;

            this.txtName.setText(this.item.Name);
            this.txtPrice.setText(Misc.formatPrice(this.item.Price, true, false));
            this.txtLikes.setText(String.valueOf(this.item.LikeCount));

            if (this.item.Available)
                this.imgStatus.setColorFilter(Color.parseColor("#3ad900"));
            else
                this.imgStatus.setColorFilter(Color.parseColor("#ec1b30"));

            Glide.with(this.itemView.getContext()).load(this.item.getDefaultPhotoPath()).into(this.imgItem);
        }
    }

    public ItemAdapter(ArrayList<Item> items, boolean compact) {
        this.items = items;
        this.compact = compact;
        this.listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RecyclerView.ViewHolder holder = (RecyclerView.ViewHolder) view.getTag();
                Item item = null;
                if (holder instanceof ItemHolder)
                    item = ((ItemHolder) holder).item;
                else
                    item = ((ItemHolderCompact) holder).item;

                Applica.Current.CurrentItem = item;
                Misc.startActivity(holder.itemView.getContext(), ItemActivity.class);
            }
        };
    }

    public void setItems(ArrayList<Item> items) {
        this.items = items;
        this.notifyDataSetChanged();
        Misc.Log("setItems %d", items.size());
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        RecyclerView.ViewHolder holder = null;

        if (this.compact) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_compact, parent, false);
            holder = new ItemHolderCompact(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
            holder = new ItemHolder(view);
        }
        view.setTag(holder);
        view.setOnClickListener(this.listener);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((IItemHolder) holder).setItem(this.items.get(position));
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }
}

class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderHolder> {
    private Chersi chersi;
    private ICommandResult iCommandResult;
    private View.OnClickListener listener;

    public static class OrderHolder extends RecyclerView.ViewHolder {
        private ChersiItem chersiItem;
        private TextView txtName;
        private TextView txtPrice;
        private TextView txtGroup;
        private TextView lblWeight;
        private TextView txtCapacity;
        private TextView txtLikes;
        private TextView txtQuantity;
        private ImageView imgItem;

        public OrderHolder(View itemView, View.OnClickListener listener) {
            super(itemView);

            this.txtName = (TextView) itemView.findViewById(R.id.txtName);
            this.txtPrice = (TextView) itemView.findViewById(R.id.txtPrice);
            this.txtGroup = (TextView) itemView.findViewById(R.id.txtGroup);
            this.lblWeight = (TextView) itemView.findViewById(R.id.lblWeight);
            this.txtCapacity = (TextView) itemView.findViewById(R.id.txtCapacity);
            this.txtLikes = (TextView) itemView.findViewById(R.id.txtLikes);
            this.txtQuantity = (TextView) itemView.findViewById(R.id.txtQuantity);
            this.imgItem = (ImageView) itemView.findViewById(R.id.imgItem);

            itemView.findViewById(R.id.txtAdd).setTag(this);
            itemView.findViewById(R.id.txtAdd).setTag(R.string.tag_one, 1);

            itemView.findViewById(R.id.txtRemove).setTag(this);
            itemView.findViewById(R.id.txtRemove).setTag(R.string.tag_one, 0);

            itemView.findViewById(R.id.txtAdd).setOnClickListener(listener);
            itemView.findViewById(R.id.txtRemove).setOnClickListener(listener);
        }

        public void setData(ChersiItem chersiItem) {
            this.chersiItem = chersiItem;

            this.txtName.setText(this.chersiItem.item.Name);
            this.txtPrice.setText(Misc.formatPrice(this.chersiItem.item.Price, true, true));
            this.txtGroup.setText(this.chersiItem.item.GroupIn.Name);
            this.lblWeight.setText(this.chersiItem.item.getMassText());
            this.txtCapacity.setText(this.chersiItem.item.getMassFormatted());
            this.txtLikes.setText(String.valueOf(this.chersiItem.item.LikeCount));
            this.txtQuantity.setText(String.valueOf(chersiItem.quantity));
            Glide.with(this.itemView.getContext()).load(this.chersiItem.item.getDefaultPhotoPath()).into(this.imgItem);
        }
    }

    public OrderAdapter(Chersi chersi, ICommandResult iCommandResult) {
        this.chersi = chersi;
        this.iCommandResult = iCommandResult;
        this.listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OrderHolder holder = (OrderHolder) view.getTag();
                int type = (int) view.getTag(R.string.tag_one);

                if (type == 0)
                    ItemDetailFragment.updateQuantity(
                            holder.chersiItem,
                            (TextView) holder.itemView.findViewById(R.id.txtQuantity),
                            null,
                            false,
                            false);
                else if (type == 1)
                    ItemDetailFragment.updateQuantity(
                            holder.chersiItem,
                            (TextView) holder.itemView.findViewById(R.id.txtQuantity),
                            null,
                            false,
                            true);
                OrderAdapter.this.notifyDataSetChanged();
                OrderAdapter.this.iCommandResult.reply(null);
            }
        };
    }

    @Override
    public OrderHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new OrderHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.card_chersi_item, parent, false),
                this.listener);
    }

    @Override
    public void onBindViewHolder(OrderHolder holder, int position) {
        holder.setData(this.chersi.items.get(position));
    }

    @Override
    public int getItemCount() {
        return this.chersi.items.size();
    }
}

class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryHolder> {
    private ArrayList<Chersi> items;

    public static class HistoryHolder extends RecyclerView.ViewHolder {
        private Chersi chersi;

        private TextView txtId;
        private TextView txtCount;
        private TextView txtPhone;
        private TextView txtName;
        private TextView txtAddress;
        private TextView txtPrice;
        private TextView txtDate;

        public HistoryHolder(View itemView) {
            super(itemView);

            this.txtId = (TextView) itemView.findViewById(R.id.txtId);
            this.txtCount = (TextView) itemView.findViewById(R.id.txtCount);
            this.txtName = (TextView) itemView.findViewById(R.id.txtName);
            this.txtPhone = (TextView) itemView.findViewById(R.id.txtPhone);
            this.txtAddress = (TextView) itemView.findViewById(R.id.txtAddress);
            this.txtPrice = (TextView) itemView.findViewById(R.id.txtPrice);
            this.txtDate = (TextView) itemView.findViewById(R.id.txtDate);
        }

        private void setData(Chersi chersi) {
            this.chersi = chersi;

            this.txtId.setText(String.valueOf(this.chersi.id));
            this.txtCount.setText(String.valueOf(this.chersi.getTotalItemsCount()));
            this.txtName.setText(this.chersi.address.Name);
            this.txtPhone.setText(this.chersi.address.Phone);
            this.txtAddress.setText(this.chersi.address.Street);
            this.txtPrice.setText(Misc.formatPrice(this.chersi.getTotalPrice(), true, true));
            this.txtDate.setText(Misc.getFormattedDateTime(this.chersi.dateTime, "yyyy/MM/dd - HH:mm"));
        }
    }

    public HistoryAdapter(ArrayList<Chersi> items) {
        this.items = items;
    }

    @Override
    public HistoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new HistoryHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_history, parent, false));
    }

    @Override
    public void onBindViewHolder(HistoryHolder holder, int position) {
        holder.setData(this.items.get(position));
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }
}

class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchHolder> {
    private SearchFragment fragment;
    private ArrayList<Item> items;
    private View.OnClickListener listener;

    public static class SearchHolder extends RecyclerView.ViewHolder {
        private Item item;
        private TextView txtName;

        public SearchHolder(View itemView) {
            super(itemView);
            this.txtName = (TextView) itemView.findViewById(R.id.txtName);
        }

        private void setData(Item item) {
            this.item = item;
            this.txtName.setText(this.item.Name);
        }
    }

    public SearchAdapter(SearchFragment fragment, ArrayList<Item> items) {
        this.fragment = fragment;
        this.items = items;
        this.listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SearchHolder holder = ((SearchHolder) view.getTag());
                Applica.Current.CurrentItem = holder.item;
                Misc.startActivity(view.getContext(), ItemActivity.class);
                SearchAdapter.this.fragment.close();
            }
        };
    }

    @Override
    public SearchHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item_search, parent, false);
        SearchHolder searchHolder = new SearchHolder(view);
        view.setOnClickListener(this.listener);
        view.setTag(searchHolder);
        return searchHolder;
    }

    @Override
    public void onBindViewHolder(SearchHolder holder, int position) {
        holder.setData(this.items.get(position));
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }
}

class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressHolder> {
    private ArrayList<Address> addresses;
    private View.OnClickListener listener;

    public static class AddressHolder extends RecyclerView.ViewHolder {
        private Address address;
        private TextView txtId;
        private TextView txtName;
        private TextView txtCity;
        private TextView txtPhone;
        private TextView txtMobile;
        private TextView txtStreet;
        private View layChecked;

        public AddressHolder(View itemView) {
            super(itemView);
            this.txtId = (TextView) itemView.findViewById(R.id.txtId);
            this.txtName = (TextView) itemView.findViewById(R.id.txtName);
            this.txtCity = (TextView) itemView.findViewById(R.id.txtCity);
            this.txtPhone = (TextView) itemView.findViewById(R.id.txtPhone);
            this.txtMobile = (TextView) itemView.findViewById(R.id.txtMobile);
            this.txtStreet = (TextView) itemView.findViewById(R.id.txtAddress);
            this.layChecked = itemView.findViewById(R.id.layChecked);
        }

        private void setData(Address address) {
            this.address = address;
            this.txtId.setText(String.valueOf(this.address.Id));
            this.txtName.setText(this.address.Name);
            this.txtCity.setText(this.address.City);
            this.txtPhone.setText(this.address.Phone);
            this.txtMobile.setText(this.address.Mobile);
            this.txtStreet.setText(this.address.Street);
            this.layChecked.setVisibility(this.address.IsDefault ? View.VISIBLE : View.GONE);
        }
    }

    public AddressAdapter(ArrayList<Address> addresses, boolean resetAll) {
        this.addresses = addresses;
        if (resetAll)
            for (int i = 0; i < this.addresses.size(); i++)
                this.addresses.get(i).IsDefault = false;


        this.listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; i < AddressAdapter.this.addresses.size(); i++)
                    AddressAdapter.this.addresses.get(i).IsDefault = false;

                AddressHolder holder = (AddressHolder) view.getTag();
                holder.address.IsDefault = true;

                AddressAdapter.this.notifyDataSetChanged();
            }
        };
    }

    @Override
    public AddressHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_address, parent, false);
        AddressHolder holder = new AddressHolder(view);
        view.setTag(holder);
        view.setOnClickListener(this.listener);
        return holder;
    }

    @Override
    public void onBindViewHolder(AddressHolder holder, int position) {
        holder.setData(this.addresses.get(position));
    }

    @Override
    public int getItemCount() {
        return this.addresses.size();
    }
}

class Converter {
    public static int parseIntOrDefault(String text, int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch (Exception ex) {
            return defaultValue;
        }
    }
}