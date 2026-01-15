package com.example.addressmatch.model;

import com.example.addressmatch.entity.TableA;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchCandidate implements Comparable<MatchCandidate> {
    private TableA tableA;
    private Double score;

    @Override
    public int compareTo(MatchCandidate other) {
        return Double.compare(other.score, this.score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchCandidate that = (MatchCandidate) o;
        return Objects.equals(tableA.getId(), that.tableA.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableA.getId());
    }
}