/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.sqlbrite.todo.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import com.example.sqlbrite.todo.R;
import com.example.sqlbrite.todo.TodoApp;
import com.example.sqlbrite.todo.db.TodoItem;
import com.jakewharton.rxbinding3.widget.RxTextView;
import com.squareup.sqlbrite3.BriteDatabase;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import javax.inject.Inject;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_NONE;
import static butterknife.ButterKnife.findById;

public final class NewItemFragment extends DialogFragment {
  private static final String KEY_LIST_ID = "list_id";

  public static NewItemFragment newInstance(long listId) {
    Bundle arguments = new Bundle();
    arguments.putLong(KEY_LIST_ID, listId);

    NewItemFragment fragment = new NewItemFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  private final PublishSubject<String> createClicked = PublishSubject.create();

  @Inject BriteDatabase db;

  private long getListId() {
    return getArguments().getLong(KEY_LIST_ID);
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    TodoApp.getComponent(activity).inject(this);
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Context context = getActivity();
    View view = LayoutInflater.from(context).inflate(R.layout.new_item, null);

    EditText name = findById(view, android.R.id.input);
    Observable.combineLatest(createClicked.toFlowable(BackpressureStrategy.LATEST)
                    .toObservable(),
            RxTextView.textChanges(name),
        new BiFunction<String, CharSequence, String>() {
          @Override public String apply(String ignored, CharSequence text) throws Throwable {
            return text.toString();
          }
        }) //
        .observeOn(Schedulers.io())
        .subscribe(new Consumer<String>() {
          @Override public void accept(String description) {
            db.insert(TodoItem.TABLE, CONFLICT_NONE,
                new TodoItem.Builder().listId(getListId()).description(description).build());
          }
        });

    return new AlertDialog.Builder(context) //
        .setTitle(R.string.new_item)
        .setView(view)
        .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            createClicked.onNext("clicked");
          }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override public void onClick(@NonNull DialogInterface dialog, int which) {
          }
        })
        .create();
  }
}
