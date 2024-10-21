import 'package:calculator/core/historyItem.dart';
import 'package:calculator/core/singletons/logger.dart';
import 'package:sqflite/sqflite.dart';

class Persistance{
  static Database? _db;
  static const String _dbName="calculator";
  static const String _tableName="calculator_history";
  static const String _historyTableCreateQuery='''
        CREATE TABLE $_tableName (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          query TEXT NOT NULL,
          created_at INTEGER NOT NULL,
          updated_at INTEGER NOT NULL
        )
      ''';

  static final Persistance _instance = Persistance._();

  Persistance._();

  factory Persistance(){
    return _instance;
  }

  static initDB() async{
    _db = await openDatabase(_dbName,
        singleInstance: true,
        version: 1,
    onOpen: (db) => LoggerSingleton().logger.i("database created : ${db.path}"),
    onCreate: (db, version)  async => await db.execute(_historyTableCreateQuery));
  }

  addHistoryItem({required HistoryItem historyItem}) async{
    assert(_db!=null);
    return await _db!.rawInsert('''
      INSERT INTO $_tableName(query, created_at, updated_at) 
      VALUES(?, ?,?)
      ''',[historyItem.query,historyItem.created_at,historyItem.updated_at]);
  }

  getHistoryItems() async{
    assert(_db!=null);
    final List<Map<String,Object?>> historyItemsRaw = await _db!.rawQuery("SELECT * from $_tableName");
    return historyItemsRaw.map((historyItemRaw) => HistoryItem.fromMap(historyItemRaw)).toList();
  }

  void dispose()async{
    await _db?.close();
  }
}