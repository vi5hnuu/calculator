import 'package:calculator/core/historyItem.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:logger/logger.dart';

class HistoryTile extends StatelessWidget {
  final VoidCallback? onEdit;

  const HistoryTile({
    super.key,
    this.onEdit,
    required this.historyItem,
  });

  final HistoryItem historyItem;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        GestureDetector(
          onTap: onEdit,
          child: Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              Flexible(child: Text(
                historyItem.query,
                textAlign: TextAlign.end,
                style: const TextStyle(fontSize: 24),
                softWrap: true,
                overflow: TextOverflow.visible,
              ),),
              const SizedBox(width: 6),
              Icon(Icons.edit_note,color: onEdit!=null ? Colors.blue : Colors.grey, size: 18),
            ],
          ),
        ),
        Row(
          mainAxisAlignment: MainAxisAlignment.end,
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.check_circle,color: Colors.green,size: 12),
                  const SizedBox(width: 4),
                  Text(
                    "Created On : ${DateTimeFormat.onlyDate(DateTime.fromMillisecondsSinceEpoch(historyItem.created_at))}",
                    textAlign: TextAlign.right,
                    style: const TextStyle(fontSize: 9),
                  )]),
            const SizedBox(width: 9),
            Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.update,color: Colors.blue, size: 12),
                  const SizedBox(width: 4),
                  Text(
                    "Updated On : ${DateTimeFormat.onlyDate(DateTime.fromMillisecondsSinceEpoch(historyItem.updated_at))}",
                    textAlign: TextAlign.right,
                    style: const TextStyle(fontSize: 9),
                  )])
          ],
        )
      ],
    );
  }
}