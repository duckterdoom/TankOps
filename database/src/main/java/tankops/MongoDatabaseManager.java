package tankops;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDatabaseManager {
    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public MongoDatabaseManager() {

        //String connectionString = "mongodb://localhost:27017"; test local
        String connectionString = "mongodb+srv://admin:0913596688@tankops.cijhhq7.mongodb.net/?compressors=zlib";

        this.mongoClient = MongoClients.create(connectionString);
        this.database = mongoClient.getDatabase("tankops_db");
        System.out.println("✅ Đã kết nối thành công tới MongoDB Atlas!");
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}