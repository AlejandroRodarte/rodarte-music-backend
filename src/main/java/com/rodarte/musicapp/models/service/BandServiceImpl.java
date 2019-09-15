package com.rodarte.musicapp.models.service;

import com.rodarte.musicapp.models.dao.AlbumDao;
import com.rodarte.musicapp.models.dao.BandDao;
import com.rodarte.musicapp.models.dao.BandViewDao;
import com.rodarte.musicapp.models.entity.Album;
import com.rodarte.musicapp.models.entity.Band;
import com.rodarte.musicapp.models.entity.views.BandView;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BandServiceImpl implements BandService {

    @Autowired
    private BandDao bandDao;

    @Autowired
    private BandViewDao bandViewDao;

    @Autowired
    private AlbumDao albumDao;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<BandView> getBands(
        Integer page,
        Integer size,
        String sort,
        String name,
        String country,
        List<String> yearRange,
        List<String> albumRange,
        List<String> songRange
    ) {

        String[] sortArr = sort.split(":");

        String sortParam = sortArr[0];
        String sortDirection = sortArr[1];

        Sort.Direction direction;

        if (sortDirection.equals("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }

        Page<BandView> bands = bandViewDao.findAllBySearchParams(
            name,
            country,
            yearRange == null ? null : Integer.parseInt(yearRange.get(0)),
            yearRange == null ? null : Integer.parseInt(yearRange.get(1)),
            albumRange == null ? null : Integer.parseInt(albumRange.get(0)),
            albumRange == null ? null : Integer.parseInt(albumRange.get(1)),
            songRange == null ? null : Integer.parseInt(songRange.get(0)),
            songRange == null ? null : Integer.parseInt(songRange.get(1)),
            PageRequest.of(page, size, Sort.by(direction, sortParam))
        );

        return bands;

    }

    @Override
    @Transactional(readOnly = true)
    public BandView getBand(Long id) {
        return bandViewDao.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Band saveBand(Band band) {

        List<Long> albumIds = new ArrayList<>();
        List<Album> albums = new ArrayList<>();

        if (band.getAlbums() != null) {
            albumIds = band.getAlbums().stream().map(Album::getId).collect(Collectors.toList());
        }

        for (Long albumId : albumIds) {

            Optional<Album> album = albumDao.findById(albumId);

            if (album.isEmpty()) {
                throw new RuntimeException("Album not found. Operation cancelled.");
            }

            if (album.get().getBand() != null) {
                throw new RuntimeException("Album " + album.get().getName() + " is already associated with a band. Operation cancelled.");
            }

            albums.add(album.get());

        }

        band.setAlbums(albums);

        Band savedBand = bandDao.save(band);

        for (Album album : albums) {
            album.setBand(band);
            albumDao.save(album);
        }

        return savedBand;

    }

    @Override
    @Transactional
    public void deleteBandById(Long id) {
        bandDao.deleteById(id);
    }

    @Override
    @Transactional
    public Integer albumCountByBandId(Long bandId) {
        return albumDao.countAlbumsByBandId(bandId);
    }

}
