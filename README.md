## Streaming Text Processing Service
A highly optimized, low-footprint Java utility designed to parse text streams into structured XML or CSV formats. Words within sentences are automatically sorted in alphabetical order (case-insensitive).

This solution features a Multi-Pass Streaming Architecture that ensures predictable memory usage and linear I/O complexity under a strict 32MB heap constraint.

## Architectural Constraints Met



### 1. 32MB Heap Limit Compliance

Traditional text processing tools parse datasets by reading files completely into memory or using an Object Tree (DOM parsing). That causes heap consumption to scale linearly with file sizes O(N) memory complexity), leading to immediate `OutOfMemoryError` crashes on large inputs.



This service eliminates memory scaling issues by implementing a **Streaming Pipeline**:

* **Sequential Extraction:** The parsing engine scans incoming streams utilizing high-performance array block buffers, processing structural units safely without heap explosions.

* **Immediate I/O Flushing:** Sentences are processed, locally sorted, converted to text lines, and immediately piped downstream using a `BufferedWriter`. Once written, elements leave the heap, keeping a flat memory footprint (~5–10MB) across gigabyte-scale inputs.

* **Multi-Pass Strategy (`ReaderSupplier`):** To correctly construct CSV structural table headers, the engine requires knowledge of the maximum word count before writing data rows. Instead of storing lines in memory, a custom `ReaderSupplier` abstracts file reopening between passes, enabling stateless streaming while supporting CSV header precomputation.



### 2. Smart Algorithmic Sentence Boundary Detection (Zero Hardcoding)

* **Non-Terminal Punctuation Scans:** A punctuation character (`.`, `!`, `?`) is algoritmically treated as part of an inline token and *not* a sentence break if it is immediately followed by an alphanumeric character without a space divider (e.g., `Mr.Young`, `i.e.`, `w.o.r.d`).

* **Generic Structural Title Filtering:** A standalone period (`.`) is prevented from triggering a boundary terminal split if the current word buffer context matches structural characteristics of initials or global title honorifics (containing only alphabetical letters with a length between 1 and 3 characters, such as `Mr.`, `Dr.`, `St.`, `vs.`, `A.`).

* This approach achieves zero-allocation evaluation constraints while natively supporting new titles or initials without requiring code alterations.



### 3. Behavioral Flexibility (Strategy Pattern)

To keep data conversion formats completely modular and decoupled from the parsing logic, the program uses a clean **Strategy Design Pattern**:

* `OutputStrategy`: The interface defining standard markup structural milestones.

* `XmlOutputStrategy`: Handles generation of structural elements and character escaping (`&amp;`, `&lt;`, `&gt;`, etc.).

* `CsvOutputStrategy`: Handles row indexing, structural padding, and dynamic header calculation.



### 4. Service Architecture Class Diagram

The class structure below illustrates the clean decoupling of our parsing engine from the presentation layer using the **Strategy Design Pattern**:



![Service Class Diagram](docs/text-processor-uml.png)



## 📂 Project Structure
	text-processing-service/
	├── pom.xml                               
	├── README.md                             
	└── src/
    ├── main/java/com/textprocessor/
    │   ├── TextProcessorApplication.java    # CLI Driver & I/O management
    │   ├── model/
    │   │   └── Sentence.java                # Immutable sorted token container
    │   ├── parser/
    │   │   ├── StreamingTextParser.java     # Core parser with DI for abbreviations
    │   │   ├── TokenHandler.java            # Event interface for parsing hooks
    │   │   └── MaxWordsHandler.java         # Pass 1 statistics collector
    │   ├── service/
    │   │   └── TextProcessingService.java   # Multi-pass lifecycle orchestrator
    │   └── output/strategy/
    │       ├── OutputStrategy.java          # Base interface
    │       ├── XmlOutputStrategy.java       # XML serialization
    │       └── CsvOutputStrategy.java       # CSV formatting logic
    
# Sample files for verification under: src/main/resources   
##How to Run
#Build

mvn clean install

##Execution

java -jar target/text-processing-service-0.0.1-SNAPSHOT.jar xml src/main/resources/small.in small-generate.xml

##Direct File Output:

java -jar target/text-processing-service-0.0.1-SNAPSHOT.jar xml input.txt output.xml

##Standard Output Redirection:

java -jar target/text-processing-service-0.0.1-SNAPSHOT.jar csv input.txt > output.csv

##Sample Output
#XML Mode:

	<text>
    <sentence>
        <word>a</word>
        <word>had</word>
        <word>lamb</word>
    </sentence>
	</text>
#CSV Mode:

	, Word 1, Word 2, Word 3
	Sentence 1, a, had, lamb

