fn main() {
    let mut number: i32 = 42;

    println!(
        "value={} size={} address={:p}",
        number,
        std::mem::size_of_val(&number),
        &number
    );

    // TODO: Assign 1000 to number, then print the same three observations.

    number = 1000;
    
    println!(
        "value={} size={} address={:p}",
        number,
        std::mem::size_of_val(&number),
        &number
    );
}
