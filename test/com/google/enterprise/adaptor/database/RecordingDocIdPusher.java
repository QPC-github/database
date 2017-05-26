// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.adaptor.database;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import com.google.enterprise.adaptor.Acl;
import com.google.enterprise.adaptor.DocId;
import com.google.enterprise.adaptor.DocIdPusher;
import com.google.enterprise.adaptor.ExceptionHandler;
import com.google.enterprise.adaptor.GroupPrincipal;
import com.google.enterprise.adaptor.Principal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

//Copied from the Adaptor Library

/**
 * A fake implementation of {@link DocIdPusher} that simply records
 * the values it receives. This implementation is not thread-safe.
 */
public class RecordingDocIdPusher implements DocIdPusher {
  private List<DocId> ids = new ArrayList<DocId>();
  private List<Record> records = new ArrayList<Record>();
  private Map<DocId, Acl> namedResources = new TreeMap<DocId, Acl>();
  private Map<GroupPrincipal, Collection<Principal>> groups
      = new TreeMap<GroupPrincipal, Collection<Principal>>();

  // Methods copied from AbstractDocIdPusher.
  @Override
  public DocId pushDocIds(Iterable<DocId> docIds)
      throws InterruptedException {
    return pushDocIds(docIds, null);
  }

  @Override
  public DocId pushDocIds(Iterable<DocId> docIds,
                          ExceptionHandler handler)
      throws InterruptedException {
    List<Record> records = new ArrayList<Record>();
    for (DocId docId : docIds) {
      records.add(new Record.Builder(docId).build());
    }
    Record record = pushRecords(records, handler);
    return record == null ? null : record.getDocId();
  }

  @Override
  public Record pushRecords(Iterable<Record> records)
      throws InterruptedException {
    return pushRecords(records, null);
  }

  @Override
  public DocId pushNamedResources(Map<DocId, Acl> resources)
      throws InterruptedException {
    return pushNamedResources(resources, null);
  }

  @Override
  public GroupPrincipal pushGroupDefinitions(
      Map<GroupPrincipal, ? extends Collection<Principal>> defs,
      boolean caseSensitive) throws InterruptedException {
    return pushGroupDefinitions(defs, caseSensitive, null);
  }
  // End of methods copied from AbstractDocIdPusher.

  /**
   * Records the records and their {@link DocId} values.
   *
   * @return {@code null}, to indicate success
   */
  @Override
  public Record pushRecords(Iterable<Record> records, ExceptionHandler handler)
      throws InterruptedException {
    for (Record record : records) {
      ids.add(record.getDocId());
      this.records.add(record);
    }
    return null;
  }

  /**
   * Records the named resources.
   *
   * @return {@code null}, to indicate success
   */
  @Override
  public DocId pushNamedResources(Map<DocId, Acl> resources,
      ExceptionHandler handler) throws InterruptedException {
    namedResources.putAll(resources);
    return null;
  }

  /**
   * Records the group definitions.
   *
   * <p>If a membership {@link Collection} for a group is a {@link List}
   * or {@link Set}, then a copy of the collection is made that
   * preserves order and equality.
   *
   * @return {@code null}, to indicate success
   */
  @Override
  public GroupPrincipal pushGroupDefinitions(
      Map<GroupPrincipal, ? extends Collection<Principal>> defs,
      boolean caseSensitive, ExceptionHandler handler)
      throws InterruptedException {
    // Make a defensive copy of each group, which just requires a copy
    // of each group's collection of members. To preserve equality, we
    // must copy each List to a List, and each Set to a Set. Other JDK
    // Collection types use Object equality, and are not copied.
    for (GroupPrincipal key : defs.keySet()) {
      Collection<Principal> members = defs.get(key);
      if (members instanceof List) {
        groups.put(key, unmodifiableList(new ArrayList<Principal>(members)));
      } else if (members instanceof Set) {
        groups.put(key, unmodifiableSet(new LinkedHashSet<Principal>(members)));
      } else {
        groups.put(key, members);
      }
    }
    return null;
  }

  /**
   * Gets an unmodifiable list of the accumulated {@link DocId DocIds}
   * passed to any of the {@code pushDocIds} or {@code pushRecords}
   * methods.
   *
   * @return an unmodifiable list of the recorded {@link DocId DocIds}
   */
  public List<DocId> getDocIds() {
    return Collections.unmodifiableList(ids);
  }

  /**
   * Gets an unmodifiable list of the accumulated {@link Record Records}
   * passed to any of the {@code pushRecords} methods.
   *
   * @return an unmodifiable list of the recorded {@link Record Records}
   */
  public List<Record> getRecords() {
    return Collections.unmodifiableList(records);
  }

  /**
   * Gets an unmodifiable map of the accumulated named resources
   * passed to any of the {@code pushNamedResources} methods. In cases
   * where the same {@link DocId} has been passed multiple times in
   * different calls to {@code pushNamedResources}, the most recently
   * pushed one is included in the returned map.
   *
   * @return an unmodifiable map of the recorded named resources
   */
  public Map<DocId, Acl> getNamedResources() {
    return Collections.unmodifiableMap(namedResources);
  }

  /**
   * Gets an unmodifiable map of the accumulated group definitions
   * passed to any of the {@code pushGroupDefinitions} methods. In
   * cases where the same {@link GroupPrincipal} has been passed
   * multiple times in different calls to {@code pushGroupDefinitions},
   * the most recently pushed one is included in the returned map.
   *
   * @return an unmodifiable map of the recorded group definitions
   */
  public Map<GroupPrincipal, Collection<Principal>> getGroupDefinitions() {
    return Collections.unmodifiableMap(groups);
  }

  /** Clears all of the recorded data. */
  public void reset() {
    ids.clear();
    records.clear();
    namedResources.clear();
    groups.clear();
  }
}
