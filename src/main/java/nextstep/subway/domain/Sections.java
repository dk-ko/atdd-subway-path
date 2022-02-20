package nextstep.subway.domain;


import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Embeddable
public class Sections {
    public static final int INVALID_FIRST_SECTION_ERROR = 1;


    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    public Sections() {
    }

    public Sections(List<Section> sections) {
        this.sections = sections;
    }

    public void addSection(Section sectionToAdd) {
        final Optional<Section> sectionFromDownStation = this.getSectionFromDownStation(sectionToAdd.getDownStation());
        if (sectionFromDownStation.isPresent()) {
            validateDistance(sectionToAdd, sectionFromDownStation);
            sectionFromDownStation.get().updateDownStation(sectionToAdd.getUpStation());
        }

        this.sections.add(sectionToAdd);
    }

    private void validateDistance(Section section, Optional<Section> sectionFromDownStation) {
        if (sectionFromDownStation.get().getDistance() <= section.getDistance()) {
            throw new IllegalArgumentException("노선 길이가 부족합니다");
        }
    }

    public List<Station> getAllStations() {
        List<Station> result = new ArrayList<>();
        Section firstSection = this.findFirstSection()
                .orElseThrow(() -> new IllegalArgumentException("호선의 첫번째 노선을 찾을 수 없습니다."));

        result.add(firstSection.getUpStation());
        Station start = firstSection.getDownStation();
        result.add(start);

        while(true) {
            final Optional<Section> findSection = this.getSectionFromUpStation(start);
            if (!findSection.isPresent()) {
                break;
            }
            final Station downStation = findSection.get().getDownStation();
            result.add(downStation);
            start = downStation;
        }
        return result;
    }

    private Optional<Section> findFirstSection() {
        final List<Station> allUpStations = this.getAllUpStations();
        final List<Station> allDownStations = this.getAllDownStations();

        final Optional<Station> optionalUpStation = allUpStations.stream()
                .filter(upStation -> !allDownStations.contains(upStation))
                .findFirst();
        if (optionalUpStation.isPresent()) {
            return this.getSectionFromUpStation(optionalUpStation.get());
        }
        return Optional.empty();
    }

    public void deleteSection(Station station) {
        if (sections.size() <= INVALID_FIRST_SECTION_ERROR) {
            throw new IllegalArgumentException("구간이 하나인 노선은 삭제할 수 없습니다.");
        }

        final Optional<Section> sectionFromUpStation = this.getSectionFromUpStation(station);
        final Optional<Section> sectionFromDownStation = this.getSectionFromDownStation(station);

        if (sectionFromUpStation.isPresent() && this.isFirstStationFrom(sectionFromUpStation.get())) {
            this.sections.remove(sectionFromUpStation.get());
            return;
        }

        if (sectionFromDownStation.isPresent() && this.isLastStationFrom(sectionFromDownStation.get())) {
            this.sections.remove(sectionFromDownStation.get());
            return;
        }

        final Section targetSection = this.getSectionFromDownStation(station)
                .orElseThrow(() -> new IllegalArgumentException("section 을 찾을 수 없습니다."));
        final Section previousSection = sectionFromUpStation
                .orElseThrow(() -> new IllegalArgumentException("section 을 찾을 수 없습니다."));
        final Section nextSection = sectionFromDownStation
                .orElseThrow(() -> new IllegalArgumentException("section 을 찾을 수 없습니다."));

        previousSection.updateUpStation(nextSection.getUpStation());
        previousSection.addDistance(nextSection.getDistance());
        this.sections.remove(targetSection);
    }


    public boolean isEmpty() {
        return this.sections.isEmpty();
    }

    public int size() {
        return this.sections.size();
    }

    public Optional<Section> getSectionFromUpStation(Station upStation) {
        return sections.stream()
                .filter(section -> section.isUpStation(upStation))
                .findFirst();
    }

    public Optional<Section> getSectionFromDownStation(Station downStation) {
        return sections.stream()
                .filter(section -> section.isDownStation(downStation))
                .findFirst();
    }

    public boolean isFirstStationFrom(Section section) {
        final Station upStation = section.getUpStation();
        return getAllUpStations().contains(upStation) && !getAllDownStations().contains(upStation);
    }

    public boolean isLastStationFrom(Section section) {
        final Station downStation = section.getDownStation();
        return !getAllUpStations().contains(downStation) && getAllDownStations().contains(downStation);
    }


    private List<Station> getAllUpStations() {
        return this.sections.stream()
                .map(Section::getUpStation)
                .collect(Collectors.toList());
    }

    private List<Station> getAllDownStations() {
        return this.sections.stream()
                .map(Section::getDownStation)
                .collect(Collectors.toList());
    }
}
