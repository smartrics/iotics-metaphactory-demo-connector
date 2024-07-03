package smartrics.iotics.samples.cars;

import java.util.List;
import java.util.Optional;

public record Binding(String pointID, String pointName, String valueID, String valueKey) {

    static Optional<Binding> find(List<Binding> bindings, String valueKey) {
        return bindings.stream().filter(binding ->
                binding.valueKey.equals(valueKey)).findFirst();
    }

    static List<Binding> filter(List<Binding> bindings, String pointName) {
        return bindings.stream().filter(binding ->
                binding.pointName.equals(pointName)).toList();
    }
}

