/**
 *      Copyright (C) 2010 EdgyTech Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.edgytech.umongo;

import com.edgytech.swingfast.XmlUnit;
import com.mongodb.BasicDBList;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import java.util.List;
import java.util.logging.Level;
import javax.swing.ImageIcon;

/**
 *
 * @author antoine
 */
public class MongoNode extends BaseTreeNode {

    Mongo mongo;
    List<String> dbs;

    public MongoNode(Mongo mongo, List<String> dbs) {
        this.mongo = mongo;
        this.dbs = dbs;
        try {
            xmlLoad(Resource.getXmlDir(), Resource.File.mongoNode, null);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        markStructured();
    }

    public Mongo getMongo() {
        return mongo;
    }

    @Override
    protected void populateChildren() {
        // first ask list of db, will also trigger discovery of nodes
        List<String> dbnames = null;
        try {
            dbnames = mongo.getDatabaseNames();
        } catch (Exception e) {
        }

        List<ServerAddress> addrs = mongo.getServerAddressList();
        
        if (addrs.size() <= 1) {
            // check if mongos
            boolean added = false;
            ServerAddress addr = addrs.get(0);
            ServerNode node = new ServerNode(mongo);
            try {
                CommandResult res = node.getServerDB().command("isdbgrid");
                if (res.ok()) {
                    addChild(new RouterNode(addr, mongo));
                    added = true;
                }
            } catch (Exception e) {}

            // could be replset of 1, check
            try {
                CommandResult res = node.getServerDB().command("isMaster");
                if (res.containsField("setName")) {
                    addChild(new ReplSetNode(mongo.getReplicaSetStatus().getName(), mongo));
                    added = true;
                }
            } catch (Exception e) {}
            
            if (!added)
                addChild(node);
        } else {
            addChild(new ReplSetNode(mongo.getReplicaSetStatus().getName(), mongo));
        }

        if (dbs != null) {
            // user specified list of DB
            dbnames = dbs;
        }

        if (dbnames != null) {
            // get all DBs to populate map
            for (String dbname : dbnames) {
                mongo.getDB(dbname);
            }
        }

        // get local and remote dbs
        for (DB db : mongo.getUsedDatabases()) {
            addChild(new DbNode(db));
        }
    }

    @Override
    protected void updateNode(List<ImageIcon> overlays) {
        label = "Mongo";
        List list = mongo.getDatabaseNames();
        label += " (" + list.size() + ")";
    }

    BasicDBList getShards() {
        XmlUnit child = getChild(0);
        if (child instanceof RouterNode) {
            return ((RouterNode)child).getShards();
        }
        return null;
    }
    
    String[] getShardNames() {
        XmlUnit child = getChild(0);
        if (child instanceof RouterNode) {
            return ((RouterNode)child).getShardNames();
        }
        return null;        
    }
}