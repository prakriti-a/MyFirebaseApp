package com.prakriti.firebaseapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class ViewSharedPosts extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
// click user to view shared post
// long click user to delete

    private FirebaseAuth firebaseAuth;

    private ListView listview_sharedUsers;
    private ArrayList<String> usersSharedList;
    private ArrayAdapter arrayAdapter;

    private ImageView img_sharedImage;
    private TextView txt_sharedMessage;

    private ArrayList<DataSnapshot> dataSnapshots; // create arraylist of data snapshots of child of received_posts folder

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_shared_posts);

        firebaseAuth = FirebaseAuth.getInstance();

        listview_sharedUsers = findViewById(R.id.listview_sharedUsers);
        usersSharedList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, usersSharedList);

        listview_sharedUsers.setAdapter(arrayAdapter);
        listview_sharedUsers.setOnItemClickListener(this);
        listview_sharedUsers.setOnItemLongClickListener(this);

        img_sharedImage = findViewById(R.id.img_sharedImage);
        txt_sharedMessage = findViewById(R.id.txt_sharedMessage);

        dataSnapshots = new ArrayList<>();

        getPostsSharedByUsers();
    }

    private void getPostsSharedByUsers() {
        // access uid of current user, then posts sent to user
        FirebaseDatabase.getInstance().getReference().child("my_users")
                .child(firebaseAuth.getCurrentUser().getUid())
                .child("received_posts").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // add snapshot to arraylist
                dataSnapshots.add(snapshot);

                String fromUser = snapshot.child("fromUser").getValue().toString(); // access key fromUser
                usersSharedList.add(fromUser);
                arrayAdapter.notifyDataSetChanged();
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) { // removed snapshot is passed
                // when child is deleted from db
                int i = 0;
                for(DataSnapshot snap : dataSnapshots) {
                    if(snap.getKey().equals(snapshot.getKey())) { // if key in array is same as deleted snap's key
                        String deletedUser = snap.child("fromUser").getValue().toString();
                        dataSnapshots.remove(snap);
                        usersSharedList.remove(deletedUser);
                    }
                }
                arrayAdapter.notifyDataSetChanged();
                img_sharedImage.setImageResource(R.drawable.image);
                txt_sharedMessage.setText("");
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // display post & message from received_posts using data snapshot of clicked item
        DataSnapshot mySnapshot = dataSnapshots.get(position); // clicked item
        // access download link of image
        String downloadLink = mySnapshot.child("imageLink").getValue().toString();
        // use Picasso external library
        Picasso.get().load(downloadLink).into(img_sharedImage);
        String message = (String) mySnapshot.child("message").getValue();
        if(message.equals("")) {
            txt_sharedMessage.setVisibility(View.GONE);
        }
        else {
            txt_sharedMessage.setText(message);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_post)
                .setMessage(R.string.delete_post_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // delete from storage first as imageIdentifier is needed
                        FirebaseStorage.getInstance().getReference()
                                .child("my_images")
                                .child(dataSnapshots.get(position).child("imageIdentifier").toString())
                                .delete();

                        // delete from db
                        FirebaseDatabase.getInstance().getReference()
                                .child("my_users").child(firebaseAuth.getCurrentUser().getUid())
                                .child("received_posts")
                                .child(dataSnapshots.get(position).getKey()).removeValue();
                        // that entire particular snapshot associated with the uid (got from the arraylist) will be removed
                        // deleted from array in Child Listener fns also
                        Toast.makeText(ViewSharedPosts.this, R.string.post_deleted, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

        return false;
    }
}