class HistoryItem{
  int? id;
  String query;
  int created_at;
  int updated_at;

  HistoryItem({this.id,required this.query,required this.created_at,required this.updated_at});

  factory HistoryItem.fromMap(Map<String, Object?> historyItem){
    return HistoryItem(id:historyItem['id'] as int,query: historyItem['query'] as String, created_at: historyItem['created_at'] as int, updated_at: historyItem['updated_at'] as int);
  }
}