it('should export data to CSV and trigger download', () => {
    // Spy on the createElement method to monitor link creation
    const createElementSpy = spyOn(document, 'createElement').and.callThrough();
    
    // Spy on appendChild and removeChild to check if the link element is added and removed correctly
    const appendChildSpy = spyOn(document.body, 'appendChild').and.callThrough();
    const removeChildSpy = spyOn(document.body, 'removeChild').and.callThrough();

    // Spy on the click event of the link element
    const clickSpy = jasmine.createSpy('click');

    // Override createElement to return a mock element with a spy on the click method
    createElementSpy.and.returnValue({
      click: clickSpy,
      setAttribute: () => {},
    } as any);

    // Call the exportCsv method
    component.exportCsv();

    // Check that document.createElement was called to create the link element
    expect(createElementSpy).toHaveBeenCalledWith('a');

    // Check that the link element was appended to the document body
    expect(appendChildSpy).toHaveBeenCalled();

    // Check that the link's click event was triggered to download the file
    expect(clickSpy).toHaveBeenCalled();

    // Check that the link element was removed from the document body after the click
    expect(removeChildSpy).toHaveBeenCalled();
  });
