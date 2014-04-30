class PaddedPrimitiveNonVolatile<T> {
  long pad1;
  long pad2;
  long pad3;
  long pad4;
  long pad5;
  long pad6;
  long pad7;
  long pad8;
  T value;
  long pad11;
  long pad12;
  long pad13;
  long pad14;
  long pad15;
  long pad16;
  long pad17;
  long pad18;
  
  public PaddedPrimitiveNonVolatile(T value) {
    this.value = value;
  }
}