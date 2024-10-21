
import 'package:logger/logger.dart';

class LoggerSingleton{
  final Logger logger=Logger();
  static final LoggerSingleton _instance=LoggerSingleton._();

  LoggerSingleton._();

  factory LoggerSingleton(){
    return _instance;
  }
}